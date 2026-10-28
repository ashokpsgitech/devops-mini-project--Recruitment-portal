// TalentBridge ATS — Interactive Motion & Micro-Interactions

document.addEventListener('DOMContentLoaded', () => {
    // -------------------------------------------------------------------------
    // 1. Button Tactile Click Ripple Effect
    // -------------------------------------------------------------------------
    document.querySelectorAll('.tb-btn').forEach(button => {
        button.addEventListener('click', function (e) {
            const rect = this.getBoundingClientRect();
            const x = e.clientX - rect.left;
            const y = e.clientY - rect.top;

            const ripple = document.createElement('span');
            ripple.style.position = 'absolute';
            ripple.style.width = '20px';
            ripple.style.height = '20px';
            ripple.style.background = 'rgba(255, 255, 255, 0.4)';
            ripple.style.borderRadius = '50%';
            ripple.style.pointerEvents = 'none';
            ripple.style.transform = 'translate(-50%, -50%) scale(0)';
            ripple.style.animation = 'tbRipple 0.6s ease-out forwards';
            ripple.style.left = `${x}px`;
            ripple.style.top = `${y}px`;

            this.appendChild(ripple);
            setTimeout(() => ripple.remove(), 600);
        });
    });

    // -------------------------------------------------------------------------
    // 2. Auto-Dismiss Alert Banners with Smooth Fade
    // -------------------------------------------------------------------------
    document.querySelectorAll('.tb-alert').forEach(alert => {
        setTimeout(() => {
            alert.style.transition = 'all 0.5s cubic-bezier(0.16, 1, 0.3, 1)';
            alert.style.opacity = '0';
            alert.style.transform = 'translateY(-10px)';
            setTimeout(() => alert.remove(), 500);
        }, 5000);
    });

    // -------------------------------------------------------------------------
    // 3. Dynamic Ripple Keyframe Injection
    // -------------------------------------------------------------------------
    if (!document.getElementById('tb-ripple-keyframes')) {
        const style = document.createElement('style');
        style.id = 'tb-ripple-keyframes';
        style.textContent = `
            @keyframes tbRipple {
                to {
                    transform: translate(-50%, -50%) scale(15);
                    opacity: 0;
                }
            }
        `;
        document.head.appendChild(style);
    }
});
