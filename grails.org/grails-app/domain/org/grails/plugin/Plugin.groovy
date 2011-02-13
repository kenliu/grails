package org.grails.plugin

import org.grails.wiki.WikiPage
import org.grails.taggable.Taggable
import org.grails.comments.Commentable
import org.grails.rateable.Rateable
import org.codehaus.groovy.grails.commons.ConfigurationHolder

/*
 * author: Matthew Taylor
 */
class Plugin implements Taggable, Commentable, Rateable {

    static final def WIKIS = ['installation','description','faq','screenshots']
    static final def VERSION_PATTERN = /^(\d+(?:\.\d+)*)([\.\-\w]*)?$/

    transient cacheService
    transient pluginService
    
    String name
    String title
    String groupId = "org.grails.plugins"
    String summary
    WikiPage description
    WikiPage installation
    WikiPage faq
    WikiPage screenshots
    String author
    String authorEmail
    String currentRelease
    String documentationUrl
    String downloadUrl
    String scmUrl
    String grailsVersion        // version it was developed against
    Boolean official = false    // specifies SpringSource support
    Boolean featured = false
    boolean zombie = false
    Number avgRating
    Date dateCreated
    Date lastUpdated
    Date lastReleased

    static searchable = {
        only = [
            'name', 'title', 'summary', 'author', 'authorEmail',
            'installation','description','faq','screenshots'
        ]
        description component: true
        installation component: true
        faq component: true
        screenshots component: true
        currentRelease index: "no", store: "yes"
        grailsVersion index: "no", store: "yes"
    }

    static transients = ['avgRating', 'fisheye']

    static constraints = {
        name unique: true
        groupId nullable: false
        summary nullable: true
        description nullable: true
        installation nullable: true
        faq nullable: true
        screenshots nullable: true
        author nullable: true
        scmUrl nullable: true
        grailsVersion nullable:true, blank:true, maxLength:16
        lastReleased nullable:true
        currentRelease matches: VERSION_PATTERN
    }

    static mapping = {
        cache 'nonstrict-read-write'
        summary type: 'text'
    }
    
    def getFisheye() {
        downloadUrl ? "${ConfigurationHolder.config.plugins.fisheye}/grails-${name}" : ''
    }

    def onAddComment = { comment ->
        cacheService.flushWikiCache()
    }

    def isNewerThan(version) {
        pluginService.compareVersions(currentRelease, version) > 0
    }

    String toString() {
        "$name : $title"
    }
}
