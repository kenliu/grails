package org.grails.plugin

import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.grails.auth.User
import org.grails.content.Version
import org.grails.tags.TagNotFoundException
import org.grails.taggable.Tag
import org.grails.taggable.TagLink
import org.grails.wiki.WikiPage

class PluginService {

    static int DEFAULT_MAX = 5

    boolean transactional = true
    
    def popularPlugins(minRatings, max = DEFAULT_MAX) {
        def ratingsComparator = new PluginComparator()
        Plugin.list(cache:true, maxResults:max).findAll {
            it.ratings.size() >= minRatings
        }.sort(ratingsComparator).reverse()
    }
    
    def newestPlugins(max = DEFAULT_MAX) {
        Plugin.withCriteria {
            order('dateCreated', 'desc')
            maxResults(max)
            cache true
        }
    }
    
    def listAllPluginsWithTotal(Map args = [max: 200]) {
        return [ Plugin.list(args), Plugin.count() ]
    }

    def listFeaturedPluginsWithTotal(Map args = [max: 200]) {
        return [ Plugin.findAllByFeatured(true, args), Plugin.countByFeatured(true) ]
    }

    def listNewestPluginsWithTotal(Map args = [max: 200]) {
        args << [cache: true, sort: "dateCreated", order: "desc" ] 
        return [ Plugin.list(args), Plugin.count() ]
    }

    def listPopularPluginsWithTotal(Map args = [max: 200]) {
        // The Rateable plugin's query only accepts pagination arguments.
        def params = [:]
        if (args["max"] != null) params["max"] = args["max"]
        if (args["offset"] != null) params["offset"] = args["offset"]
        return [
                Plugin.listOrderByAverageRating(cache:true, *:params),
                Plugin.countRated() ]
    }

    def listRecentlyUpdatedPluginsWithTotal(Map args = [max: 200]) {
        args << [cache: true, sort: "lastReleased", order: "desc" ] 
        return [ Plugin.list(args), Plugin.count() ]
    }

    def listSupportedPluginsWithTotal(Map args = [max: 200]) {
        return [ Plugin.findAllByOfficial(true, args), Plugin.countByOfficial(true) ]
    }

    def listPluginsByTagWithTotal(Map args = [max: 200], String tagName) {
        // Start by grabbing the tag with the given name. There's unlikely to
        // be frequent changes to the 'tags' table, so caching the query makes
        // sense.
        def tag = Tag.findByName(tagName, [cache: true])

        // Make sure that there is a tag with this name. If there isn't we have
        // to notify the caller via an exception.
        if (!tag) throw new TagNotFoundException(tagName)

        // Now find all the plugins with this tag.
        def result = []
        def links = TagLink.findAllByTagAndType(tag, 'plugin', args += [cache: true])
        if (links) {
            result << Plugin.withCriteria {
                inList 'id', links*.tagRef
                cache true

                if (args.offset) firstResult args.offset
                if (args.max) maxResults args.max
            }

            result << Plugin.withCriteria {
                inList 'id', links*.tagRef
                cache true

                projections {
                    rowCount()
                }
            }
        }
        else {
            result << [] << 0
        }

        return result
    }
    
    def runMasterUpdate() {
        translateMasterPlugins(generateMasterPlugins())
    }
    
    def generateMasterPlugins() {
        try {
            def pluginLoc = ConfigurationHolder.config?.plugins?.pluginslist
            def listFile = new URL(pluginLoc)
            def listText = listFile.text
            // remove the first line of <?xml blah/>
            listText = listText.replaceAll(/\<\?xml ([^\<\>]*)\>/, '')
            def plugins = new XmlSlurper().parseText(listText)

            log.info "Found ${plugins.plugin.size()} master plugins."

            plugins.plugin.inject([]) { pluginsList, pxml ->
                if (!pxml.release.size()) return pluginsList
                def latestRelease = pxml.@'latest-release'
                def latestReleaseNode = pxml.release.find { releaseNode ->
                    releaseNode.@version == latestRelease
                }
                def p = new Plugin()
                p.with {
                    name = pxml.@name
                    grailsVersion = (latestReleaseNode.documentation.toString().startsWith('http://grails.org') ? getGrailsVersion(p) : '')
                    title = latestReleaseNode.title.toString() ?: pxml.@name
                    description = new WikiPage(body:latestReleaseNode.description.toString() ?: '')
                    author = latestReleaseNode.author
                    authorEmail = latestReleaseNode.authorEmail
                    documentationUrl = replaceOldDocsWithNewIfNecessary(latestReleaseNode.documentation, name)
                    downloadUrl = latestReleaseNode.file
                    currentRelease = latestRelease
                }

                pluginsList << p
            }            
        }catch(e) {
            log.error "Error parsing master plugin list: ${e.message}",e
        }
    }

    private def replaceOldDocsWithNewIfNecessary(oldDocs, name) {
        boolean match = oldDocs =~ /http:\/\/(www\.)?grails.org\//
        return match ? "http://grails.org/plugin/${name}" : oldDocs
    }

    def translateMasterPlugins(masters) {
        Plugin.withSession { session ->
            masters.each { master ->
                try {
                    def plugin = Plugin.findByName(master.name)
                    if (!plugin) {
                        // injecting a unique wiki page name for description
                        // pull off the desc so we don't try to save it
                        def descWiki = master.description
                        master.description = null
                        // so we need to save the master first to get its id
                        if (!master.save()) {
                            log.error "Could not save master plugin: $master.name ($master.title), version $master.currentRelease"
                            master.errors.allErrors.each { log.error "\t$it" }

                        }
                        // put the wiki page back with a unique title
                        descWiki.title = "description-${master.id}"
                        master.description = descWiki
                        log.info "No existing plugin, creating new ==> ${master.name}"
                        // before saving the master, we need to save the description wiki page
                        if (!master.description.save() && master.description.hasErrors()) {
                            master.description.errors.allErrors.each { log.error it }
                        } else {
                            def v = master.description.createVersion()
                            v.author = User.findByLogin('admin')
                            if(!v.save(flush:true)) {
                                log.warn "Can't save version ${v.title} (${v.number})"
                                v.errors.allErrors.each { log.warn it }
                            }
                        }
                        //inject dummy wikis for users to fill in
                        (Plugin.WIKIS - 'description').each { wiki ->
                            master."$wiki" = new WikiPage(title:"$wiki-${master.id}", body:'')
                            assert master."$wiki".save()
                        }
                        // give an initial release date of now
                        master.lastReleased = new Date()
                        // save new master plugin
                        if (!master.save()) {
                            log.error "Could not save master plugin: $master.name ($master.title), version $master.currentRelease"
                            master.errors.allErrors.each { log.error "\t$it" }
                        } else {
                            log.info "New plugin was saved from master: $master.name"
                            log.info "There are now ${Plugin.count()} plugins."
                        }
                    } else {
                        // update existing plugin
                        updatePlugin(plugin, master)
                    }
                    
                }
                finally {
                    session.flush()
                    session.clear()
                }
            }
            
        }
    }

    def updatePlugin(plugin, master) {
        log.info "Updating plugin \"$plugin.name\"..."

        // these attributes are overriden by local plugin domain changes
        updatePluginAttribute('title', plugin, master)
        updatePluginAttribute('author', plugin, master)
        updatePluginAttribute('authorEmail', plugin, master)
        
        // these are always overridden by the master list
        plugin.name = master.name
        plugin.documentationUrl = master.documentationUrl
        plugin.downloadUrl = master.downloadUrl
        // if this was a release update, also update the date of release
        if (plugin.currentRelease != master.currentRelease) {
            plugin.lastReleased = new Date();
        }
        plugin.currentRelease = master.currentRelease
        plugin.grailsVersion = master.grailsVersion

        if (!plugin.save()) {
            log.warn "Local plugin '$plugin.name' was not updated properly... errors follow:"
            plugin.errors.allErrors.each { log.warn it }
        // I don't know why new versions need to be created here, but it's causing
        // problems because each new Version has the same number as the current
        // wiki page version. PAL
//        } else {
//            def v = plugin.description.createVersion()
//            v.author = User.findByLogin('admin')
//            if(!v.save(flush:true)) {
//                log.warn "Can't save version ${v.title} (${v.number})"
//                v.errors.allErrors.each { log.warn it }
//            }
        }
        
        log.info "Local plugin '$plugin.name' was updated with master version."
    }
    
    def updatePluginAttribute(propName, plugin, master) {
        if (master."$propName" && !plugin."$propName") {
            plugin."$propName" = master."$propName"
        }
    }

    def resolvePossiblePlugin(wiki) {
        // WikiPages that are actually components of a Plugin should be treated as a Plugin
        if (wiki.title.matches(/(${Plugin.WIKIS.join('|')})-[0-9]*/)) {
            // we're returning the actual parent Plugin object instead of the WikiPage, but we'll make the body
            // of the WikiPage available on this Plugin object so the view can render it as if it were a real
            // WikiPage by calling on the 'body' attributed
            def plugin = Plugin.read(wiki.title.split('-')[1].toLong())
            if (!plugin) {
                log.warn "There should be a plugin with id ${wiki.title.split('-')[1]} to match WikiPage ${wiki.title}, but there is not."
                return null
            }
            plugin.metaClass.getBody = { -> wiki.body }
            return plugin
        }
        wiki
    }

    def compareVersions(v1, v2) {
        def v1Num = new PluginVersion(version:v1)
        def v2Num = new PluginVersion(version:v2)
        v1Num.compareTo(v2Num)
    }

    def getGrailsVersion(plugin) {
        def xmlLoc = "${ConfigurationHolder.config?.plugins?.location}/grails-${plugin.name}/tags/LATEST_RELEASE/plugin.xml"
        def xmlUrl = new URL(xmlLoc)
        def xmlText = xmlUrl.text

        def pluginXml = new XmlSlurper().parseText(xmlText)
        pluginXml.@grailsVersion.toString()
    }

    /**
     * Text-based search using the given Lucene-compatible query string.
     * Returns a list of Plugin instances, although they may not be fully
     * hydrated, i.e. any non-searchable properties will not be populated.
     * @param query The Lucene-compatible query string.
     * @param options A map of search modifiers, such as 'sort', 'offset'
     * and 'max'.
     */
    protected final search(String query, Map options) {
        return searchWithResults(query, options).results
    }

    /**
     * Text-based search using the given Lucene-compatible query string.
     * Returns a list of Plugin instances, although they may not be fully
     * hydrated, i.e. any non-searchable properties will not be populated.
     * @param query The Lucene-compatible query string.
     * @param category The category of plugin to constrain the search to:
     * 'featured', 'newest', 'recentlyUpdated', 'supported'. Note that
     * 'popular' is not currently supported by text-based search.
     * @param options A map of search modifiers, such as 'sort', 'offset'
     * and 'max'.
     */
    protected final search(String query, String category, Map options) {
        query = categoryToSearchConstraint(category) + " " + query
        options << optionsForCategory(category)

        return searchWithResults(query, options).results
    }

    /**
     * Same as {@link #search(String, Map)} except it supports the options
     * as named arguments.
     */
    protected final search(Map options, String query) {
        return searchWithResults(query, options).results
    }

    /**
     * Text-based search using the given Lucene-compatible query string.
     * Returns a tuple containing the list of Plugin instances matching
     * the query and the total number of results. The plugins objects
     * may not be fully hydrated, i.e. any non-searchable properties will
     * not be populated.
     * @param query The Lucene-compatible query string.
     * @param options A map of search modifiers, such as 'sort', 'offset'
     * and 'max'.
     */
    protected final searchWithTotal(String query, Map options) {
        def results = searchWithResults(query, options)
        return [results.results, results.total]
    }

    /**
     * Text-based search using the given Lucene-compatible query string.
     * Returns a tuple containing the list of Plugin instances matching
     * the query and the total number of results. The plugins objects
     * may not be fully hydrated, i.e. any non-searchable properties will
     * not be populated.
     * @param query The Lucene-compatible query string.
     * @param category The category of plugin to constrain the search to:
     * 'featured', 'newest', 'recentlyUpdated', 'supported'. Note that
     * 'popular' is not currently supported by text-based search.
     * @param options A map of search modifiers, such as 'sort', 'offset'
     * and 'max'.
     */
    protected final searchWithTotal(String query, String category, Map options) {
        query = categoryToSearchConstraint(category) + " " + query
        options << optionsForCategory(category)

        def results = searchWithResults(query, options)
        return [results.results, results.total]
    }

    /**
     * Same as {@link #searchWithTotal(String, Map)} except it supports the
     * options as named arguments.
     */
    protected final searchWithTotal(Map options, String query) {
        def results = searchWithResults(query, options)
        return [results.results, results.total]
    }

    /**
     * Executes a Searchable search and returns the results object.
     */
    private searchWithResults(String query, Map options = [:]) {
        return Plugin.search(query, options)
    }

    /**
     * Returns a map of search options based on the given category. These
     * search options can be used to override those provided in a normal
     * search. If the category has no requirements on the search options,
     * this method returns an empty map.
     */
    private Map optionsForCategory(String category) {
        switch(category.toLowerCase()) {
        case "newest":
            return [sort: "dateCreated", order: "desc"] 

        case "recentlyUpdate":
            return [sort: "lastReleased", order: "desc"] 

        default:
            return ""
        }
    }

    /**
     * Given a category, this method returns a query fragment that can be
     * attached to an existing Lucence-compatible query string to constrain
     * the results to plugins within that category.
     */
    private String categoryToSearchConstraint(String category) {
        switch(category.toLowerCase()) {
        case "featured":
            return "+featured:true"

        case "supported":
            return "+official:true"

        default:
            return ""
        }
    }
}

class PluginVersion implements Comparable {

    String[] version
    String tag

    public void setVersion(versionString) {
        def split = versionString.split(/[-|_]/)
        version = split[0].split(/\./)
        tag = split.size() > 1 ? split[1] : ''
    }

    public int compareTo(Object o) {
        def result = null
        version.eachWithIndex { versionElem, i ->
            // skip if we've already found a result in a previous index
            if (result != null) return

            // if this version is a snapshot and the other is not, the other is always greater
            if (tag && !o.tag) {
                result = -1
                return
            }
            
            // if the other is a snapshot and this is not, this version is always greater
            if (o.tag && !tag) {
                result = 1
                return
            }

            // make other version 0 if there really is no placeholder for it
            def otherVersion = (o.version.size() == i) ? 0 : o.version[i]

            if (versionElem > otherVersion) {
                result = 1
                return
            }
            if (versionElem < otherVersion) {
                result = -1
                return
            }
        }
        // if the comparison is equal at this point, and there are more elements on the other version, then that version
        // will be greater because it has another digit on it, otherwise the two really are equal
        if (result == null) {
            if (o.version.size() > version.size()) result = -1
            else result = 0
        }
        result
    }   
}

// sorts by averageRating, then number of votes
class PluginComparator implements Comparator {
    public int compare(Object o1, Object o2) {
        if (o1.averageRating > o2.averageRating) return 1
        if (o1.averageRating < o2.averageRating) return -1
        // averateRatings are same, so use number of votes
        if (o1.ratings.size() > o2.ratings.size()) return 1
        if (o1.ratings.size() < o2.ratings.size()) return -1
        return 0
    }
}
