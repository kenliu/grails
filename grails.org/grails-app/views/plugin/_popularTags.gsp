<h2>Popular Tags</h2>
<h4><g:link controller='tag' action='cloud'>Tag Cloud</g:link></h4>
<ul id="popularTags">
    <g:each var='tag' in="${tags}">
        <li>
            <g:link controller="plugin" action="browseByTag" params="[tagName: tag[0]]">${tag[0]} (${tag[1]})</g:link>
        </li>
    </g:each>
</ul>
