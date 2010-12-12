<g:setProvider library="yui"/>

<g:set var="updateElement" value="${update ?: 'contentPane'}"/>

<div id="infoLinks" style="margin-left:450px;">
    <g:remoteLink update="${updateElement}" controller="content" id="${content?.title}" params="[xhr:true]"><img src="${createLinkTo(dir:'images/','icon-edit.png')}" width="15" height="15" alt="Icon Edit" class="inlineIcon" border="0" /></g:remoteLink>
    <g:remoteLink update="${updateElement}" controller="content" id="${content?.title}" params="[xhr:true]">View Page</g:remoteLink>

    <shiro:authenticated>
        <g:remoteLink update="editPane" controller="content" action="markupWikiPage" id="${content?.title}" ><img src="${createLinkTo(dir:'images/','icon-edit.png')}" width="15" height="15" alt="Icon Edit" class="inlineIcon" border="0" /></g:remoteLink>
        <g:remoteLink update="editPane" controller="content" action="markupWikiPage" id="${content?.title}" >View Markup</g:remoteLink>
    </shiro:authenticated>

</div>


<h3>Page: ${content?.title}, Version:${content?.number}</h3>

<div id="editPane">
    <wiki:text>
        ${content?.body}
    </wiki:text>

</div>
