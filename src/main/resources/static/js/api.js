// API Helper Functions
// Authentication is handled via HttpOnly cookies - no localStorage token storage
const api = {
    baseUrl: '/api',

    async request(endpoint, options = {}) {
        const headers = {
            'Content-Type': 'application/json',
            ...options.headers
        };

        const method = options.method || 'GET';
        console.log(`[API] ${method} ${endpoint}`);

        try {
            const response = await fetch(`${this.baseUrl}${endpoint}`, {
                ...options,
                headers,
                credentials: 'include'  // Always send cookies with requests
            });

            console.log(`[API] ${method} ${endpoint} -> ${response.status}`);

            if (!response.ok) {
                // Redirect to login if unauthorized (401) or forbidden (403) for protected actions
                if (response.status === 401 || response.status === 403) {
                    // Only redirect for write operations (POST, PUT, DELETE) or protected endpoints
                    const isWriteOperation = options.method && options.method !== 'GET';
                    const isProtectedEndpoint = endpoint.includes('/me') ||
                                               endpoint.includes('/saved') ||
                                               endpoint.includes('/subscribe') ||
                                               endpoint.includes('/unsubscribe');

                    if (isWriteOperation || isProtectedEndpoint) {
                        console.warn(`[API] Auth required for ${endpoint}, redirecting to login`);
                        window.location.href = '/login';
                        return;
                    }
                }

                const error = await response.json().catch(() => ({ message: 'Request failed' }));
                console.error(`[API] ${method} ${endpoint} failed:`, error.message || response.status);
                throw new Error(error.message || `HTTP ${response.status}`);
            }

            if (response.status === 204) {
                return null;
            }

            const data = await response.json();
            console.log(`[API] ${method} ${endpoint} -> Success, data:`, Array.isArray(data) ? `${data.length} items` : 'object');
            return data;
        } catch (error) {
            console.error(`[API] ${method} ${endpoint} error:`, error);
            throw error;
        }
    },

    // Auth
    async login(username, password) {
        return this.request('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ username, password })
        });
    },

    async register(username, email, password) {
        return this.request('/auth/register', {
            method: 'POST',
            body: JSON.stringify({ username, email, password })
        });
    },

    async logout() {
        await this.request('/auth/logout', { method: 'POST' }).catch(() => {});
        window.location.href = '/';
    },

    async getCurrentUser() {
        try {
            return await this.request('/users/me');
        } catch {
            return { authenticated: false };
        }
    },

    // Posts
    async getPosts(sort = 'hot', page = 0, size = 25) {
        return this.request(`/posts?sort=${sort}&page=${page}&size=${size}`);
    },

    async getPostsBySub(subName, sort = 'hot', page = 0, size = 25) {
        return this.request(`/posts/subs/${subName}?sort=${sort}&page=${page}&size=${size}`);
    },

    async getPost(id) {
        return this.request(`/posts/${id}`);
    },

    async createPost(data) {
        return this.request('/posts', {
            method: 'POST',
            body: JSON.stringify(data)
        });
    },

    async getHotPosts() {
        return this.request('/posts/hot');
    },

    async getNewPosts() {
        return this.request('/posts/new');
    },

    async getTopPosts() {
        return this.request('/posts/top');
    },

    // Subs (communities)
    async getPopularSubs() {
        return this.request('/subs');
    },

    async getSub(name) {
        return this.request(`/subs/${name}`);
    },

    async createSub(data) {
        return this.request('/subs', {
            method: 'POST',
            body: JSON.stringify(data)
        });
    },

    async subscribe(subId) {
        return this.request(`/subs/${subId}/subscribe`, { method: 'POST' });
    },

    async unsubscribe(subId) {
        return this.request(`/subs/${subId}/unsubscribe`, { method: 'POST' });
    },

    // Comments
    async getComments(postId) {
        return this.request(`/comments/post/${postId}`);
    },

    async createComment(postId, content, parentCommentId = null) {
        return this.request('/comments', {
            method: 'POST',
            body: JSON.stringify({ postId, content, parentCommentId })
        });
    },

    // Votes
    async vote(targetId, targetType, voteType) {
        return this.request('/votes', {
            method: 'POST',
            body: JSON.stringify({ targetId, targetType, voteType })
        });
    },

    // Users
    async getUser(username) {
        return this.request(`/users/${username}`);
    },

    async getUserPosts(userId, page = 0, size = 25) {
        return this.request(`/posts/user/${userId}?page=${page}&size=${size}`);
    },

    async updateProfile(updates) {
        return this.request('/users/me', {
            method: 'PUT',
            body: JSON.stringify(updates)
        });
    },

    async changePassword(currentPassword, newPassword) {
        return this.request('/users/me/change-password', {
            method: 'POST',
            body: JSON.stringify({ currentPassword, newPassword })
        });
    },

    async forgotPassword(email) {
        return this.request('/users/forgot-password', {
            method: 'POST',
            body: JSON.stringify({ email })
        });
    },

    async resetPassword(token, newPassword) {
        return this.request('/users/reset-password', {
            method: 'POST',
            body: JSON.stringify({ token, newPassword })
        });
    },

    // Post actions
    async updatePost(id, updates) {
        return this.request(`/posts/${id}`, {
            method: 'PUT',
            body: JSON.stringify(updates)
        });
    },

    async deletePost(id) {
        return this.request(`/posts/${id}`, {
            method: 'DELETE'
        });
    },

    async searchPosts(query, page = 0, size = 25) {
        return this.request(`/posts/search?q=${encodeURIComponent(query)}&page=${page}&size=${size}`);
    },

    async savePost(id) {
        return this.request(`/posts/${id}/save`, { method: 'POST' });
    },

    async hidePost(id) {
        return this.request(`/posts/${id}/hide`, { method: 'POST' });
    },

    async getSavedPosts(page = 0, size = 25) {
        return this.request(`/posts/saved?page=${page}&size=${size}`);
    },

    // Comment actions
    async updateComment(id, content) {
        return this.request(`/comments/${id}`, {
            method: 'PUT',
            body: JSON.stringify({ content })
        });
    },

    async deleteComment(id) {
        return this.request(`/comments/${id}`, {
            method: 'DELETE'
        });
    },

    // Subs moderation
    async updateSub(id, data) {
        return this.request(`/subs/${id}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    },

    async updateSubRules(id, rules) {
        return this.request(`/subs/${id}/rules`, {
            method: 'PUT',
            body: JSON.stringify(rules)
        });
    },

    async addFlair(subId, flair) {
        return this.request(`/subs/${subId}/flairs`, {
            method: 'POST',
            body: JSON.stringify(flair)
        });
    },

    async removeFlair(subId, flairName) {
        return this.request(`/subs/${subId}/flairs/${flairName}`, {
            method: 'DELETE'
        });
    },

    async searchSub(query) {
        return this.request(`/subs/search?q=${encodeURIComponent(query)}`);
    },

    // User moderated/created subs
    async getModeratedSubs(username) {
        return this.request(`/users/${username}/moderated-subs`);
    },

    async getCreatedSubs(username) {
        return this.request(`/users/${username}/created-subs`);
    }
};
