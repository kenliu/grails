<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
    <title>New Plugin</title>
    <meta content="subpage" name="layout"/>
    <link rel="stylesheet" href="${createLinkTo(dir:'css',file:'plugins.css')}" />
</head>
<body>
<div id="contentPane">
    <ul id="infoLinks">
        <li class='home'>
            <g:link controller="plugin" action="index">Plugins Home</g:link><br/>
        </li>
    </ul>
    <h1>Create Plugin</h1>
    <g:if test="${flash.message}">
        <div class="message">${flash.message}</div>
    </g:if>
    <g:hasErrors bean="${plugin}">
        <div class="errors">
            <g:renderErrors bean="${plugin}" as="list"/>
        </div>
    </g:hasErrors>
    <g:form name="createPlugin" url="[controller:'plugin', action:'createPlugin']">
        <div class="dialog">
            <table>
                <tbody>

                <plugin:input
                        name="Name"
                        description="This is what you would type in the command line after 'grails install-plugin'.">
                    <input type="text" id="name" name="name" value="${fieldValue(bean: plugin, field: 'name')}"/>
                </plugin:input>

                <plugin:input
                        name="Title"
                        description="The 'Human-Readable' title of your plugin">
                    <input type="text" id="title" name="title" value="${fieldValue(bean: plugin, field: 'title')}"/>
                </plugin:input>

                <plugin:input
                        name="Author"
                        description="Plugin author's name(s)">
                    <input type="text" id="author" name="author" value="${fieldValue(bean: plugin, field: 'author')}"/>
                </plugin:input>

                <plugin:input
                        name="Author Email"
                        description="Plugin author email addresses">
                    <input type="text" id="authorEmail" name="authorEmail" value="${fieldValue(bean: plugin, field: 'authorEmail')}"/>
                </plugin:input>

                <plugin:input
                        name="Grails Version"
                        description="The Grails version this plugin was developed under.">
                    <input type="text" id="grailsVersion" name="grailsVersion" value="${fieldValue(bean: plugin, field: 'grailsVersion')}"/>
                </plugin:input>

                <plugin:input
                        name="Current Release"
                        description="Current plugin release">
                    <input type="text" id="currentRelease" name="currentRelease" value="${fieldValue(bean: plugin, field: 'currentRelease')}"/>
                </plugin:input>

                <plugin:input
                        name="Documentation URL"
                        description="Where on the web is are your docs?  If this is it, then leave blank.">
                    <input type="text" id="documentationUrl" name="documentationUrl" value="${fieldValue(bean: plugin, field: 'documentationUrl')}"/>
                </plugin:input>

                <plugin:input
                        name="Download URL"
                        description="Where someone would click to get your plugin">
                    <input type="text" id="downloadUrl" name="downloadUrl" value="${fieldValue(bean: plugin, field: 'downloadUrl')}"/>
                </plugin:input>

                </tbody>
            </table>
        </div>
        <g:submitButton name="save" value="Save"/>
    </g:form>
</div>
</body>
</html>
