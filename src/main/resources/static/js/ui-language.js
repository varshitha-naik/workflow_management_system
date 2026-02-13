/**
 * UI Terminology Helper
 * Centralizes all user-facing language mappings to ensure consistent, professional, non-technical terminology.
 */
const UILanguage = {
    roles: {
        'TENANT_ADMIN': 'Tenant Admin',
        'TENANT_MANAGER': 'Manager',
        'USER': 'User',
        'GLOBAL_ADMIN': 'Platform Admin',
        'SUPER_ADMIN': 'Super Admin'
    },
    status: {
        'IN_PROGRESS': 'In Progress',
        'APPROVED': 'Approved',
        'REJECTED': 'Rejected',
        'COMPLETED': 'Completed',
        'PENDING': 'Pending Action',
        'DRAFT': 'Draft'
    },
    actions: {
        'APPROVE': 'Approve Request',
        'REJECT': 'Reject Request',
        'SUBMIT': 'Submit Request',
        'CANCEL': 'Cancel Request',
        'SAVE': 'Save Draft'
    },
    // Contextual messages for workflow steps
    stepMessages: {
        waitingForYou: 'Your action is required.',
        waitingForOthers: 'Waiting for {role} review.',
        completed: 'Process completed successfully.'
    },

    // Helper to get display text
    getRoleDisplay(roleCode) {
        return this.roles[roleCode] || roleCode;
    },

    getStatusDisplay(statusCode) {
        return this.status[statusCode] || statusCode;
    },

    // Generates a user-friendly status message based on context
    getStepStatusMessage(stepName, requiredRole, isMyTurn) {
        const roleDisplay = this.getRoleDisplay(requiredRole);

        if (isMyTurn) {
            return {
                title: 'Action Required',
                description: this.stepMessages.waitingForYou,
                badgeClass: 'badge-warning'
            };
        } else {
            return {
                title: `${stepName} Pending`,
                description: this.stepMessages.waitingForOthers.replace('{role}', roleDisplay),
                badgeClass: 'badge-info'
            };
        }
    }
};

// Expose globally
window.UILanguage = UILanguage;
