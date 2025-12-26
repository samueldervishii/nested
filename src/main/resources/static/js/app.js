// Main Application JavaScript

// Global state
let currentUser = null;
let currentSort = 'hot';
const DEBUG = false; // Set to true for development

// ============================================
// Dark Mode Support
// ============================================
function initTheme() {
    const savedTheme = localStorage.getItem('theme') || 'light';
    document.documentElement.setAttribute('data-theme', savedTheme);
    updateThemeToggle(savedTheme);
    updateLogo(savedTheme);
}

function toggleTheme() {
    const currentTheme = document.documentElement.getAttribute('data-theme');
    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-theme', newTheme);
    localStorage.setItem('theme', newTheme);
    updateThemeToggle(newTheme);
    updateLogo(newTheme);
}

function updateThemeToggle(theme) {
    const toggleBtn = document.getElementById('theme-toggle');
    if (toggleBtn) {
        toggleBtn.textContent = theme === 'dark' ? '☀️' : '🌙';
        toggleBtn.title = theme === 'dark' ? 'Switch to Light Mode' : 'Switch to Dark Mode';
    }
}

function updateLogo(theme) {
    const logos = document.querySelectorAll('.logo img');
    logos.forEach(logo => {
        if (theme === 'dark') {
            logo.src = '/images/nested-logo-dark.svg';
        } else {
            logo.src = '/images/nested-logo.svg';
        }
    });
}

// ============================================
// Markdown Parser (Simple Implementation)
// ============================================
function parseMarkdown(text) {
    if (!text) return '';

    let html = escapeHtml(text);

    // Code blocks (```code```) - must be before inline code
    html = html.replace(/```([\s\S]*?)```/g, '<pre><code>$1</code></pre>');

    // Inline code (`code`)
    html = html.replace(/`([^`]+)`/g, '<code>$1</code>');

    // Headers (### Header)
    html = html.replace(/^### (.+)$/gm, '<h3>$1</h3>');
    html = html.replace(/^## (.+)$/gm, '<h2>$1</h2>');
    html = html.replace(/^# (.+)$/gm, '<h1>$1</h1>');

    // Bold (**text** or __text__)
    html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
    html = html.replace(/__(.+?)__/g, '<strong>$1</strong>');

    // Italic (*text* or _text_)
    html = html.replace(/\*([^*]+)\*/g, '<em>$1</em>');
    html = html.replace(/_([^_]+)_/g, '<em>$1</em>');

    // Strikethrough (~~text~~)
    html = html.replace(/~~(.+?)~~/g, '<del>$1</del>');

    // Blockquotes (> quote)
    html = html.replace(/^&gt; (.+)$/gm, '<blockquote>$1</blockquote>');

    // Horizontal rule (---)
    html = html.replace(/^---$/gm, '<hr>');

    // Links [text](url)
    html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>');

    // Unordered lists (- item or * item)
    html = html.replace(/^[\-\*] (.+)$/gm, '<li>$1</li>');
    html = html.replace(/(<li>.*<\/li>\n?)+/g, '<ul>$&</ul>');

    // Line breaks
    html = html.replace(/\n\n/g, '</p><p>');
    html = html.replace(/\n/g, '<br>');

    // Wrap in paragraph if not already wrapped
    if (!html.startsWith('<')) {
        html = '<p>' + html + '</p>';
    }

    return html;
}

// Utility: Debounce function for search/inputs
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// Utility: Prevent double-clicks on buttons
function setLoading(element, loading) {
    if (loading) {
        element.disabled = true;
        element.dataset.originalText = element.textContent;
        element.style.opacity = '0.6';
    } else {
        element.disabled = false;
        element.style.opacity = '1';
    }
}

// Initialize app
document.addEventListener('DOMContentLoaded', async () => {
    // Initialize theme first (prevents flash)
    initTheme();

    // Load current user
    currentUser = await api.getCurrentUser();
    updateAuthArea();

    // Setup sort tabs
    setupSortTabs();

    // Setup search
    setupSearch();

    // Load initial content based on page
    const path = window.location.pathname;
    const urlParams = new URLSearchParams(window.location.search);

    if (path === '/search') {
        const query = urlParams.get('q');
        if (query) {
            document.getElementById('search-input').value = query;
            searchPosts(query);
        }
    } else if (path === '/saved') {
        loadSavedPosts();
    } else if (path === '/c/popular') {
        loadPopularFeed();
        loadPopularsub();
    } else if (path === '/c/all') {
        loadAllFeed();
        loadPopularsub();
    } else if (path === '/my-communities') {
        loadMyCommunities();
    } else if (path === '/' || path === '') {
        loadHomeFeed();
        loadPopularsub();
    }
});

// Update auth area in header
function updateAuthArea() {
    const authArea = document.getElementById('auth-area');
    if (!authArea) return;

    const themeToggle = '<button id="theme-toggle" class="theme-toggle" onclick="toggleTheme()" title="Toggle Dark Mode">🌙</button>';

    if (currentUser && currentUser.authenticated) {
        authArea.innerHTML = `
            <span class="user-info">
                <a href="/u/${currentUser.username}">${currentUser.username}</a>
                <span class="user-karma">(${currentUser.karma} karma)</span>
            </span>
            <a href="/saved">saved</a>
            <a href="/settings">settings</a>
            <a href="#" onclick="api.logout(); return false;">logout</a>
            ${themeToggle}
        `;
    } else {
        authArea.innerHTML = `
            <a href="/login">login</a> or <a href="/register">register</a>
            ${themeToggle}
        `;
    }

    // Update toggle button state
    updateThemeToggle(localStorage.getItem('theme') || 'light');
}

// Setup sort tabs
function setupSortTabs() {
    const tabs = document.querySelectorAll('.nav-tab');
    const urlParams = new URLSearchParams(window.location.search);
    currentSort = urlParams.get('sort') || 'hot';

    tabs.forEach(tab => {
        if (tab.dataset.sort === currentSort) {
            tab.classList.add('active');
        } else {
            tab.classList.remove('active');
        }

        tab.addEventListener('click', (e) => {
            e.preventDefault();
            currentSort = tab.dataset.sort;
            tabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');

            // Update URL
            const url = new URL(window.location);
            url.searchParams.set('sort', currentSort);
            window.history.pushState({}, '', url);

            // Reload content
            const path = window.location.pathname;
            if (path.startsWith('/n/')) {
                const subName = path.split('/')[2];
                loadSubPosts(subName);
            } else {
                loadHomeFeed();
            }
        });
    });
}

// Load home feed
async function loadHomeFeed() {
    const postList = document.getElementById('post-list');
    if (!postList) return;

    postList.innerHTML = '<div class="loading">Loading posts...</div>';

    try {
        let posts;
        switch (currentSort) {
            case 'new':
                posts = await api.getNewPosts();
                break;
            case 'top':
                posts = await api.getTopPosts();
                break;
            default:
                posts = await api.getHotPosts();
        }
        renderPosts(posts, postList);
    } catch (error) {
        postList.innerHTML = '<div class="loading">Failed to load posts. <a href="/">Try again</a></div>';
    }
}

// Load popular feed (same as top posts for now)
async function loadPopularFeed() {
    const postList = document.getElementById('post-list');
    if (!postList) return;

    postList.innerHTML = '<div class="loading">Loading popular posts...</div>';

    try {
        const posts = await api.getTopPosts();
        renderPosts(posts, postList);
    } catch (error) {
        postList.innerHTML = '<div class="loading">Failed to load posts. <a href="/">Try again</a></div>';
    }
}

// Load all feed (new posts from all communities)
async function loadAllFeed() {
    const postList = document.getElementById('post-list');
    if (!postList) return;

    postList.innerHTML = '<div class="loading">Loading all posts...</div>';

    try {
        const posts = await api.getNewPosts();
        renderPosts(posts, postList);
    } catch (error) {
        postList.innerHTML = '<div class="loading">Failed to load posts. <a href="/">Try again</a></div>';
    }
}

// Load popular communities
async function loadPopularsub() {
    const container = document.getElementById('popular-sub');
    if (!container) return;

    try {
        const communities = await api.getPopularSubs();

        if (communities.length === 0) {
            container.innerHTML = '<p style="font-size: 12px; color: #888;">No communities yet. <a href="sub/create">Create one!</a></p>';
            return;
        }

        container.innerHTML = communities.map(sub => `
            <div style="margin-bottom: 8px;">
                <a href="/n/${sub.name}" style="font-weight: bold;">n/${sub.name}</a>
                <span style="color: #888; font-size: 11px;">${sub.subscriberCount} subscribers</span>
            </div>
        `).join('');
    } catch (error) {
        container.innerHTML = '<p style="color: #888; font-size: 12px;">Failed to load</p>';
    }
}

// Load my communities page
async function loadMyCommunities() {
    const ownedContainer = document.getElementById('owned-communities');
    const subscribedContainer = document.getElementById('subscribed-communities');

    if (!currentUser || !currentUser.authenticated) {
        if (ownedContainer) {
            ownedContainer.innerHTML = '<div class="empty-state"><p>Please <a href="/login">login</a> to see your communities.</p></div>';
        }
        if (subscribedContainer) {
            subscribedContainer.innerHTML = '';
            document.getElementById('subscribed-section').style.display = 'none';
        }
        return;
    }

    let ownedSubIds = new Set();

    // Load owned/created communities first
    if (ownedContainer) {
        try {
            const createdSubs = await api.getCreatedSubs(currentUser.username);
            // Store owned sub IDs to filter from subscriptions
            createdSubs.forEach(sub => ownedSubIds.add(sub.id));

            if (createdSubs.length === 0) {
                ownedContainer.innerHTML = '<div class="empty-state"><p>You haven\'t created any communities yet.</p><a href="/sub/create">Create your first community</a></div>';
            } else {
                ownedContainer.innerHTML = createdSubs.map(sub => renderCommunityCard(sub, true)).join('');
            }
        } catch (error) {
            console.error('Failed to load created communities:', error);
            ownedContainer.innerHTML = '<div class="empty-state"><p>Failed to load communities.</p></div>';
        }
    }

    // Load subscribed communities (excluding owned ones)
    if (subscribedContainer) {
        try {
            const subscribedSubs = await api.getSubscriptions();
            // Filter out communities the user owns
            const filteredSubs = subscribedSubs.filter(sub => !ownedSubIds.has(sub.id));

            if (filteredSubs.length === 0) {
                subscribedContainer.innerHTML = '<div class="empty-state"><p>You haven\'t joined any other communities yet.</p><a href="/">Browse communities</a></div>';
            } else {
                subscribedContainer.innerHTML = filteredSubs.map(sub => renderCommunityCard(sub, false)).join('');
                // Add leave button handlers
                subscribedContainer.querySelectorAll('.leave-btn').forEach(btn => {
                    btn.addEventListener('click', handleLeaveCommunity);
                });
            }
        } catch (error) {
            console.error('Failed to load subscribed communities:', error);
            subscribedContainer.innerHTML = '<div class="empty-state"><p>Failed to load communities.</p></div>';
        }
    }
}

// Render community card
function renderCommunityCard(sub, isOwner) {
    const iconHtml = sub.iconUrl
        ? `<img src="${sub.iconUrl}" alt="${sub.name}">`
        : sub.name.charAt(0).toUpperCase();

    return `
        <a href="/n/${sub.name}" class="community-card">
            <div class="community-icon">${iconHtml}</div>
            <div class="community-info">
                <div class="community-name">
                    n/${escapeHtml(sub.name)}
                    ${isOwner ? '<span class="owner-badge">Owner</span>' : ''}
                </div>
                <div class="community-meta">${sub.subscriberCount} members</div>
            </div>
            ${!isOwner ? `
                <div class="community-actions">
                    <button class="leave-btn" data-sub-id="${sub.id}" onclick="event.preventDefault(); event.stopPropagation();">Leave</button>
                </div>
            ` : ''}
        </a>
    `;
}

// Handle leave community
async function handleLeaveCommunity(e) {
    e.preventDefault();
    e.stopPropagation();

    const btn = e.currentTarget;
    const subId = btn.dataset.subId;

    if (!confirm('Are you sure you want to leave this community?')) {
        return;
    }

    try {
        await api.unsubscribe(subId);
        const card = btn.closest('.community-card');
        if (card) {
            card.style.animation = 'fadeOut 0.3s ease';
            setTimeout(() => {
                card.remove();
                // Check if list is now empty
                const container = document.getElementById('subscribed-communities');
                if (container && container.children.length === 0) {
                    container.innerHTML = '<div class="empty-state"><p>You haven\'t joined any communities yet.</p><a href="/">Browse communities</a></div>';
                }
            }, 300);
        }
        showNotification('Left community successfully');
    } catch (error) {
        showNotification('Failed to leave community', 'error');
    }
}

// Render posts
function renderPosts(posts, container) {
    if (!posts || posts.length === 0) {
        container.innerHTML = '<div class="loading">No posts yet. <a href="/submit">Be the first to post!</a></div>';
        return;
    }

    container.innerHTML = posts.map((post, index) => renderPost(post, index + 1)).join('');

    // Add vote event listeners
    container.querySelectorAll('.vote-btn').forEach(btn => {
        btn.addEventListener('click', handleVote);
    });
}

// Render single post
function renderPost(post, rank) {
    const thumbnailHtml = getThumbnailHtml(post);
    const flairHtml = post.flair ? `<span class="post-flair ${post.flair.toLowerCase()}">${post.flair}</span>` : '';
    const subName = post.subName || '';
    const isImagePost = post.postType === 'IMAGE' || (post.imageUrls && post.imageUrls.length > 0);
    const domainHtml = post.url ? `<span class="post-domain">(${getDomain(post.url)})</span>` : (post.postType === 'TEXT' || isImagePost) ? `<span class="post-domain">(self.${subName})</span>` : '';
    const postLink = (post.postType === 'LINK' && post.url) ? post.url : `/n/${subName}/comments/${post.id}`;

    const upvoteClass = post.userVote === 1 ? 'active' : '';
    const downvoteClass = post.userVote === -1 ? 'active' : '';

    return `
        <div class="post" data-post-id="${post.id}">
            <div class="post-rank">${rank}</div>
            <div class="vote-buttons">
                <button class="vote-btn upvote ${upvoteClass}" data-target="${post.id}" data-type="POST" data-vote="UPVOTE">▲</button>
                <span class="vote-count">${formatVoteCount(post.voteCount)}</span>
                <button class="vote-btn downvote ${downvoteClass}" data-target="${post.id}" data-type="POST" data-vote="DOWNVOTE">▼</button>
            </div>
            ${thumbnailHtml}
            <div class="post-content">
                <div class="post-title">
                    <a href="${postLink}">${escapeHtml(post.title)}</a>
                    ${flairHtml}
                    ${domainHtml}
                </div>
                <div class="post-meta">
                    submitted ${post.timeAgo} by <a href="/u/${post.authorUsername}">${post.authorUsername}</a>
                    to <a href="/n/${subName}">n/${subName}</a>
                </div>
                <div class="post-actions">
                    <a href="/n/${subName}/comments/${post.id}" class="post-comments">${post.commentCount} comments</a>
                    <a href="#" onclick="sharePost('${post.id}', '${subName}'); return false;">share</a>
                    <a href="#" onclick="toggleSavePost('${post.id}'); return false;" class="save-btn-${post.id}">${post.saved ? 'unsave' : 'save'}</a>
                    <a href="#" onclick="toggleHidePost('${post.id}'); return false;">hide</a>
                    ${currentUser && currentUser.id === post.authorId ? `
                        <a href="#" onclick="editPost('${post.id}'); return false;">edit</a>
                        <a href="#" onclick="deletePost('${post.id}'); return false;">delete</a>
                    ` : ''}
                </div>
            </div>
        </div>
    `;
}

// Get thumbnail HTML
function getThumbnailHtml(post) {
    if (post.thumbnailUrl) {
        return `<div class="post-thumbnail"><img src="${post.thumbnailUrl}" alt=""></div>`;
    }

    // Show first image as thumbnail for image posts
    if (post.imageUrls && post.imageUrls.length > 0) {
        return `<div class="post-thumbnail"><img src="${post.imageUrls[0]}" alt="" style="object-fit: cover;"></div>`;
    }

    if (post.postType === 'LINK' && post.url) {
        return `<div class="post-thumbnail"><span>🔗</span></div>`;
    }

    if (post.postType === 'IMAGE') {
        return `<div class="post-thumbnail"><span>🖼️</span></div>`;
    }

    if (post.postType === 'VIDEO') {
        return `<div class="post-thumbnail"><span>▶️</span></div>`;
    }

    return `<div class="post-thumbnail self"><span>📝</span></div>`;
}

// Handle vote
async function handleVote(e) {
    if (!currentUser || !currentUser.authenticated) {
        window.location.href = '/login';
        return;
    }

    const btn = e.currentTarget;

    // Prevent double-clicks
    if (btn.disabled) return;

    const targetId = btn.dataset.target;
    const targetType = btn.dataset.type;
    const voteType = btn.dataset.vote;

    const container = btn.closest('.vote-buttons') || btn.closest('.comment');
    const upBtn = container.querySelector('.upvote');
    const downBtn = container.querySelector('.downvote');

    // Disable both buttons during request
    upBtn.disabled = true;
    downBtn.disabled = true;

    try {
        const result = await api.vote(targetId, targetType, voteType);

        // Update vote count
        const voteCountEl = container.querySelector('.vote-count');
        if (voteCountEl) {
            voteCountEl.textContent = formatVoteCount(result.voteCount);
        }

        // Update button states
        if (voteType === 'UPVOTE') {
            if (upBtn.classList.contains('active')) {
                upBtn.classList.remove('active');
            } else {
                upBtn.classList.add('active');
                downBtn.classList.remove('active');
            }
        } else {
            if (downBtn.classList.contains('active')) {
                downBtn.classList.remove('active');
            } else {
                downBtn.classList.add('active');
                upBtn.classList.remove('active');
            }
        }
    } catch (error) {
        showNotification('Vote failed', 'error');
    } finally {
        // Re-enable buttons
        upBtn.disabled = false;
        downBtn.disabled = false;
    }
}

// Load sub page
async function loadsub(subName) {
    try {
        const sub = await api.getSub(subName);

        document.getElementById('page-title').textContent = `n/${sub.name} - nested`;
        document.getElementById('sub-name').textContent = `n/${sub.name}`;
        document.getElementById('sub-description').textContent = sub.description || '';
        document.getElementById('sidebar-sub-name').textContent = `n/${sub.name}`;
        document.getElementById('sidebar-description').textContent = sub.description || 'No description available.';
        document.getElementById('subscriber-count').textContent = sub.subscriberCount.toLocaleString();
        document.getElementById('created-date').textContent = sub.createdAt;

        // Subscribe button - hide for moderators
        const subscribeBtn = document.getElementById('subscribe-btn');
        if (currentUser && currentUser.authenticated) {
            // Check if user is moderator (from backend or by checking moderatorIds)
            const isModerator = sub.isModerator ||
                (sub.moderatorIds && sub.moderatorIds.includes(currentUser.id));

            if (isModerator) {
                // Moderators don't see subscribe button
                subscribeBtn.style.display = 'none';
            } else {
                subscribeBtn.style.display = 'inline-block';
                subscribeBtn.textContent = sub.isSubscribed ? 'Unsubscribe' : 'Subscribe';
                subscribeBtn.classList.toggle('subscribed', sub.isSubscribed);
                subscribeBtn.onclick = async () => {
                    if (sub.isSubscribed) {
                        await api.unsubscribe(sub.id);
                        sub.isSubscribed = false;
                        subscribeBtn.textContent = 'Subscribe';
                        subscribeBtn.classList.remove('subscribed');
                    } else {
                        await api.subscribe(sub.id);
                        sub.isSubscribed = true;
                        subscribeBtn.textContent = 'Unsubscribe';
                        subscribeBtn.classList.add('subscribed');
                    }
                };
            }
        }

        loadSubPosts(subName);
    } catch (error) {
        document.getElementById('sub-name').textContent = 'Community not found';
    }
}

// Load sub posts
async function loadSubPosts(subName) {
    const postList = document.getElementById('post-list');
    postList.innerHTML = '<div class="loading">Loading posts...</div>';

    try {
        const posts = await api.getPostsBySub(subName, currentSort);
        renderPosts(posts, postList);
    } catch (error) {
        postList.innerHTML = '<div class="loading">Failed to load posts.</div>';
    }
}

// Load post detail page
async function loadPost(postId, subName) {
    const postDetail = document.getElementById('post-detail');
    const commentsList = document.getElementById('comments-list');

    try {
        // Load post
        const post = await api.getPost(postId);
        document.getElementById('page-title').textContent = `${post.title} : ${post.subName}`;

        const upvoteClass = post.userVote === 1 ? 'active' : '';
        const downvoteClass = post.userVote === -1 ? 'active' : '';

        postDetail.innerHTML = `
            <div style="display: flex;">
                <div class="vote-buttons" style="margin-right: 10px;">
                    <button class="vote-btn upvote ${upvoteClass}" data-target="${post.id}" data-type="POST" data-vote="UPVOTE">▲</button>
                    <span class="vote-count">${formatVoteCount(post.voteCount)}</span>
                    <button class="vote-btn downvote ${downvoteClass}" data-target="${post.id}" data-type="POST" data-vote="DOWNVOTE">▼</button>
                </div>
                <div style="flex: 1;">
                    <div class="post-title">${escapeHtml(post.title)}</div>
                    <div class="post-meta">
                        submitted ${post.timeAgo} by <a href="/u/${post.authorUsername}">${post.authorUsername}</a>
                        to <a href="/n/${post.subName}">n/${post.subName}</a>
                    </div>
                    ${post.imageUrls && post.imageUrls.length > 0 ? `
                        <div class="post-images" style="margin: 15px 0;">
                            ${post.imageUrls.map(url => `
                                <img src="${url}" alt="Post image" style="max-width: 100%; max-height: 500px; border-radius: 4px; margin-bottom: 10px; cursor: pointer;" onclick="window.open('${url}', '_blank')">
                            `).join('')}
                        </div>
                    ` : ''}
                    ${post.content ? `<div class="post-content-text markdown-content">${parseMarkdown(post.content)}</div>` : ''}
                    ${post.url ? `<div class="post-content-text"><a href="${post.url}" target="_blank">${post.url}</a></div>` : ''}
                    <div class="post-actions" style="margin-top: 10px;">
                        <span class="post-comments">${post.commentCount} comments</span>
                        <a href="#">share</a>
                        <a href="#">save</a>
                        <a href="#">hide</a>
                    </div>
                </div>
            </div>
        `;

        // Add vote listeners
        postDetail.querySelectorAll('.vote-btn').forEach(btn => {
            btn.addEventListener('click', handleVote);
        });

        // Show comment form if logged in
        if (currentUser && currentUser.authenticated) {
            document.getElementById('comment-form-container').style.display = 'block';
            document.getElementById('comment-form').onsubmit = async (e) => {
                e.preventDefault();
                const content = document.getElementById('comment-input').value;
                if (!content.trim()) return;

                try {
                    await api.createComment(postId, content);
                    document.getElementById('comment-input').value = '';
                    loadComments(postId);
                } catch (error) {
                    alert('Failed to post comment');
                }
            };
        }

        // Load sub info
        const sub = await api.getSub(subName);
        document.getElementById('sidebar-sub-name').textContent = `n/${sub.name}`;
        document.getElementById('sidebar-description').textContent = sub.description || 'No description.';
        document.getElementById('subscriber-count').textContent = sub.subscriberCount.toLocaleString();

        // Load comments
        loadComments(postId);
    } catch (error) {
        postDetail.innerHTML = '<div class="loading">Post not found.</div>';
    }
}

// Load comments
async function loadComments(postId) {
    const commentsList = document.getElementById('comments-list');

    try {
        const comments = await api.getComments(postId);
        if (comments.length === 0) {
            commentsList.innerHTML = '<p style="color: #888; padding: 10px;">No comments yet. Be the first!</p>';
            return;
        }

        commentsList.innerHTML = renderComments(comments);

        // Add vote listeners
        commentsList.querySelectorAll('.vote-btn').forEach(btn => {
            btn.addEventListener('click', handleVote);
        });

        // Add reply listeners
        commentsList.querySelectorAll('.reply-btn').forEach(btn => {
            btn.addEventListener('click', handleReply);
        });
    } catch (error) {
        commentsList.innerHTML = '<p style="color: red;">Failed to load comments.</p>';
    }
}

// Render comments recursively
function renderComments(comments) {
    return comments.map(comment => renderComment(comment)).join('');
}

function renderComment(comment) {
    const upvoteClass = comment.userVote === 1 ? 'active' : '';
    const downvoteClass = comment.userVote === -1 ? 'active' : '';
    const depth = Math.min(comment.depth, 5);

    const repliesHtml = comment.replies && comment.replies.length > 0
        ? renderComments(comment.replies)
        : '';

    return `
        <div class="comment comment-depth-${depth}" data-comment-id="${comment.id}">
            <div class="comment-header">
                <button class="vote-btn upvote ${upvoteClass}" data-target="${comment.id}" data-type="COMMENT" data-vote="UPVOTE" style="font-size: 10px;">▲</button>
                <button class="vote-btn downvote ${downvoteClass}" data-target="${comment.id}" data-type="COMMENT" data-vote="DOWNVOTE" style="font-size: 10px;">▼</button>
                <a href="/u/${comment.authorUsername}" class="comment-author">${comment.authorUsername}</a>
                <span class="vote-count">${comment.voteCount} points</span>
                <span>${comment.timeAgo}</span>
            </div>
            <div class="comment-body markdown-content">${parseMarkdown(comment.content)}</div>
            <div class="comment-actions">
                <a href="#" class="reply-btn" data-comment-id="${comment.id}" data-post-id="${comment.postId}">reply</a>
                <a href="#" onclick="shareComment('${comment.id}', '${comment.postId}'); return false;">share</a>
                ${currentUser && currentUser.id === comment.authorId && !comment.deleted ? `
                    <a href="#" onclick="editComment('${comment.id}'); return false;">edit</a>
                    <a href="#" onclick="deleteComment('${comment.id}', '${comment.postId}'); return false;">delete</a>
                ` : ''}
            </div>
            <div class="reply-form" id="reply-form-${comment.id}" style="display: none; margin: 10px 0;">
                <textarea rows="3" style="width: 100%; padding: 5px;"></textarea>
                <button class="btn-primary" style="margin-top: 5px; padding: 5px 15px;" onclick="submitReply('${comment.id}', '${comment.postId}')">Reply</button>
                <button style="margin-top: 5px; padding: 5px 15px;" onclick="cancelReply('${comment.id}')">Cancel</button>
            </div>
            ${repliesHtml}
        </div>
    `;
}

// Handle reply button click
function handleReply(e) {
    e.preventDefault();
    if (!currentUser || !currentUser.authenticated) {
        window.location.href = '/login';
        return;
    }

    const commentId = e.target.dataset.commentId;
    const replyForm = document.getElementById(`reply-form-${commentId}`);
    replyForm.style.display = replyForm.style.display === 'none' ? 'block' : 'none';
}

// Submit reply
async function submitReply(commentId, postId) {
    const replyForm = document.getElementById(`reply-form-${commentId}`);
    const textarea = replyForm.querySelector('textarea');
    const content = textarea.value.trim();

    if (!content) return;

    try {
        await api.createComment(postId, content, commentId);
        textarea.value = '';
        replyForm.style.display = 'none';
        loadComments(postId);
    } catch (error) {
        alert('Failed to post reply');
    }
}

// Cancel reply
function cancelReply(commentId) {
    document.getElementById(`reply-form-${commentId}`).style.display = 'none';
}

// Load user profile
async function loadUserProfile(username) {
    try {
        const user = await api.getUser(username);

        document.getElementById('page-title').textContent = `${user.username} - nested`;
        document.getElementById('profile-username').textContent = user.username;
        document.getElementById('user-karma').textContent = `${user.karma} karma`;
        document.getElementById('user-created').textContent = `• member since ${new Date(user.createdAt).toLocaleDateString()}`;

        // Load user's avatar
        if (user.avatarUrl) {
            document.getElementById('profile-avatar').src = user.avatarUrl;
        }

        // Load user's bio
        if (user.bio) {
            document.getElementById('profile-about').textContent = user.bio;
        }

        // Load user's posts
        const posts = await api.getUserPosts(user.id);
        const postList = document.getElementById('post-list');
        renderPosts(posts, postList);

        // Load user's moderated subs
        try {
            const moderatedSubs = await api.getModeratedSubs(username);
            if (moderatedSubs && moderatedSubs.length > 0) {
                const subsBox = document.getElementById('moderated-subs-box');
                const subsList = document.getElementById('moderated-subs-list');

                subsBox.style.display = 'block';
                subsList.innerHTML = moderatedSubs.map(sub => `
                    <div style="margin-bottom: 8px;">
                        <a href="/n/${sub.name}" style="color: #0079d3; text-decoration: none; font-weight: 500;">
                            n/${sub.name}
                        </a>
                        <span style="color: #7c7c7c; font-size: 11px; margin-left: 5px;">
                            ${sub.subscriberCount} members
                        </span>
                    </div>
                `).join('');
            }
        } catch (e) {
            // Silently fail - moderated subs are optional
        }
    } catch (error) {
        document.getElementById('profile-username').textContent = 'User not found';
    }
}

// Utility functions
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function formatVoteCount(count) {
    if (count >= 10000) {
        return (count / 1000).toFixed(1) + 'k';
    }
    return count.toString();
}

function getDomain(url) {
    try {
        const urlObj = new URL(url);
        return urlObj.hostname.replace('www.', '');
    } catch {
        return url;
    }
}

// Share post (copy link to clipboard)
async function sharePost(postId, subName) {
    const url = `${window.location.origin}/n/${subName}/comments/${postId}`;
    try {
        await navigator.clipboard.writeText(url);
        showNotification('Link copied to clipboard!');
    } catch (err) {
        prompt('Copy this link:', url);
    }
}

// Toggle save post
async function toggleSavePost(postId) {
    if (!currentUser || !currentUser.authenticated) {
        window.location.href = '/login';
        return;
    }

    try {
        const result = await api.savePost(postId);
        const btn = document.querySelector(`.save-btn-${postId}`);
        if (btn) {
            btn.textContent = result.saved ? 'unsave' : 'save';
        }
        showNotification(result.saved ? 'Post saved!' : 'Post unsaved');
    } catch (error) {
        showNotification('Failed to save post', 'error');
    }
}

// Toggle hide post
async function toggleHidePost(postId) {
    if (!currentUser || !currentUser.authenticated) {
        window.location.href = '/login';
        return;
    }

    try {
        const result = await api.hidePost(postId);
        if (result.hidden) {
            const postEl = document.querySelector(`[data-post-id="${postId}"]`);
            if (postEl) {
                postEl.style.display = 'none';
            }
            showNotification('Post hidden');
        }
    } catch (error) {
        showNotification('Failed to hide post', 'error');
    }
}

// Edit post
async function editPost(postId) {
    const postEl = document.querySelector(`[data-post-id="${postId}"]`);
    const contentEl = postEl.querySelector('.post-content-text') || postEl.querySelector('.post-content');

    const currentContent = contentEl?.textContent || '';
    const newContent = prompt('Edit your post:', currentContent);

    if (newContent !== null && newContent !== currentContent) {
        try {
            await api.updatePost(postId, { content: newContent });
            showNotification('Post updated!');
            window.location.reload();
        } catch (error) {
            showNotification('Failed to update post', 'error');
        }
    }
}

// Delete post
async function deletePost(postId) {
    if (!confirm('Are you sure you want to delete this post?')) {
        return;
    }

    try {
        await api.deletePost(postId);
        showNotification('Post deleted');
        window.location.href = '/';
    } catch (error) {
        showNotification('Failed to delete post', 'error');
    }
}

// Edit comment
async function editComment(commentId) {
    const commentEl = document.querySelector(`[data-comment-id="${commentId}"]`);
    const bodyEl = commentEl.querySelector('.comment-body');
    const currentContent = bodyEl.textContent;

    const newContent = prompt('Edit your comment:', currentContent);

    if (newContent !== null && newContent !== currentContent) {
        try {
            await api.updateComment(commentId, newContent);
            bodyEl.textContent = newContent;
            showNotification('Comment updated!');
        } catch (error) {
            showNotification('Failed to update comment', 'error');
        }
    }
}

// Delete comment
async function deleteComment(commentId, postId) {
    if (!confirm('Are you sure you want to delete this comment?')) {
        return;
    }

    try {
        await api.deleteComment(commentId);
        showNotification('Comment deleted');
        loadComments(postId);
    } catch (error) {
        showNotification('Failed to delete comment', 'error');
    }
}

// Search posts
async function searchPosts(query) {
    const postList = document.getElementById('post-list');
    if (!postList) return;

    postList.innerHTML = '<div class="loading">Searching...</div>';

    try {
        const posts = await api.searchPosts(query);
        if (posts.length === 0) {
            postList.innerHTML = `<div class="loading">No results found for "${escapeHtml(query)}"</div>`;
        } else {
            renderPosts(posts, postList);
        }
    } catch (error) {
        postList.innerHTML = '<div class="loading">Search failed. Please try again.</div>';
    }
}

// Load saved posts
async function loadSavedPosts() {
    const postList = document.getElementById('post-list');
    if (!postList) return;

    postList.innerHTML = '<div class="loading">Loading saved posts...</div>';

    try {
        const posts = await api.getSavedPosts();
        if (posts.length === 0) {
            postList.innerHTML = '<div class="loading">No saved posts yet.</div>';
        } else {
            renderPosts(posts, postList);
        }
    } catch (error) {
        postList.innerHTML = '<div class="loading">Failed to load saved posts.</div>';
    }
}

// Show notification toast
function showNotification(message, type = 'success') {
    const existingToast = document.querySelector('.toast-notification');
    if (existingToast) {
        existingToast.remove();
    }

    const toast = document.createElement('div');
    toast.className = `toast-notification ${type}`;
    toast.textContent = message;
    toast.style.cssText = `
        position: fixed;
        bottom: 20px;
        right: 20px;
        padding: 12px 24px;
        background: ${type === 'error' ? '#ff4444' : '#4CAF50'};
        color: white;
        border-radius: 4px;
        z-index: 10000;
        animation: fadeIn 0.3s ease;
    `;
    document.body.appendChild(toast);

    setTimeout(() => {
        toast.style.animation = 'fadeOut 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// Setup search functionality
function setupSearch() {
    const searchForm = document.getElementById('search-form');
    const searchInput = document.getElementById('search-input');

    if (searchForm && searchInput) {
        searchForm.addEventListener('submit', (e) => {
            e.preventDefault();
            const query = searchInput.value.trim();
            if (query) {
                window.location.href = `/search?q=${encodeURIComponent(query)}`;
            }
        });
    }
}

// Share comment link
async function shareComment(commentId, postId) {
    const url = `${window.location.origin}${window.location.pathname}#comment-${commentId}`;
    try {
        await navigator.clipboard.writeText(url);
        showNotification('Comment link copied!');
    } catch (err) {
        prompt('Copy this link:', url);
    }
}
