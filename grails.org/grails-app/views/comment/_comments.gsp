<div class="comments">
    <h2><a class='anchor' name='comments'>${comments.size()} Comments</a></h2>
    <ul id="commentList">
        <g:each var="comment" in="${comments}">
            <li class="comment">
                <g:render template="/comment/comment" var='comment' bean="${comment}"/>
            </li>
        </g:each>
    </ul>

    <div id="nextComment" class="hidden"></div>

    <div id="postComment">
        <h2>Post a Comment</h2>
        <g:if test="${!locked}">
            <g:if test="${jsec.principal()}">
                <script>
                    var handleComment = function() {
                        var lastComment = YAHOO.util.Dom.getLastChild('commentList');
                        var nextComment = YAHOO.util.Dom.get('nextComment');
                        var newComment = document.createElement('li');
                        newComment.id = 'newestComment';
                        newComment.className = 'comment';
                        YAHOO.util.Dom.setStyle(newComment, 'opacity', 0.0);
                        newComment.innerHTML = nextComment.innerHTML;
                        YAHOO.util.Dom.insertAfter(newComment, lastComment);
                        var attributes = {
                            opacity: { to: 1.0 }
                        };
                        var anim = new YAHOO.util.Anim('newestComment', attributes);
                        anim.animate();
                        newComment.id = null;
                        // clear comment textarea
                        YAHOO.util.Dom.get('comment').value = '';
                    };
                </script>
                <g:formRemote name='commentForm' url="[controller: commentType, action:'postComment', id:parentId]" update="nextComment" onComplete="handleComment()">
                    <textarea id='comment' name='comment' value=''></textarea>
                    <br/>
                    <g:submitButton name='submitComment' value='Post Comment'/>
                </g:formRemote>
            </g:if>
            <g:else>
                <g:link action='postComment' id='${parentId}'>Login</g:link> to leave a comment
            </g:else>
        </g:if>
        <g:else>
            COMMENTS ARE LOCKED.
        </g:else>
    </div>

</div>
