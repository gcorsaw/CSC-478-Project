// Base Application Context Gateway
const POSRuntime = {
    activeUser: null,
    activeRole: 'GUEST',

    async synchronizeTerminal() {
        try {
            const response = await fetch('/api/v1/terminal/identity');
            const identity = await response.json();
            if (identity.authenticated) {
                this.activeUser = identity.uid;
                this.activeRole = identity.role;
                console.log(`Connected terminal identity verified: ${this.activeRole}`);
            } else {
                this.promptTerminalLock();
            }
        } catch (err) {
            console.error("Terminal sync broken down network pipeline:", err);
        }
    },

    promptTerminalLock() {
        console.log("Terminal restricted environment active. Awaiting staff credential presentation...");
    },

    // Enforces role-based boundaries or triggers a manager override if needed
    assertPermission(requiredRole, clearAuthorizedAction, actionReasonContext, targetTableId = 0) {
        const structuralHierarchy = ['GUEST', 'HOST', 'WAITER', 'CHEF', 'MANAGER', 'OWNER'];
        const currentTier = structuralHierarchy.indexOf(this.activeRole);
        const targetTier = structuralHierarchy.indexOf(requiredRole);

        if (currentTier >= targetTier) {
            clearAuthorizedAction();
        } else {
            this.dispatchManagerApprovalTicket(targetTableId, actionReasonContext, clearAuthorizedAction);
        }
    },

    async dispatchManagerApprovalTicket(tableId, reasonText, resolutionCallback) {
        alert(`Authorization Required. Sending request to Floor Manager for: "${reasonText}"`);
        
        try {
            const res = await fetch(`/api/v1/override/raise?tableId=${tableId}&description=${encodeURIComponent(reasonText)}`, {
                method: 'POST'
            });
            const ticket = await res.json();
            
            // Poll for a supervisor decision every 3 seconds
            const executionLoop = setInterval(async () => {
                const statusCheck = await fetch(`/api/v1/override/poll/${ticket.ticketId}`);
                const liveTicket = await statusCheck.json();
                
                if (liveTicket.status === 'APPROVED') {
                    clearInterval(executionLoop);
                    alert("Supervisor authorized! Executing operation...");
                    resolutionCallback();
                } else if (liveTicket.status === 'DENIED') {
                    clearInterval(executionLoop);
                    alert("Override request denied by floor manager.");
                }
            }, 3000);

        } catch (netErr) {
            console.error("Failed to forward floor override alert up execution tree:", netErr);
        }
    }
};

document.addEventListener("DOMContentLoaded", () => POSRuntime.synchronizeTerminal());
