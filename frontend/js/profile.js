let profileSnapshot = null;

async function loadProfile() {
    try {
        profileSnapshot = await getJson("/profile");
        renderProfile(profileSnapshot);
    } catch (error) {
        showMessage(error.message, "error");
    }
}

function renderProfile(profile) {
    const form = document.getElementById("profile-form");
    if (!form || !profile) return;
    form.realName.value = profile.realName || "";
    form.phone.value = profile.phone || "";
    form.email.value = profile.email || "";
    form.avatarUrl.value = profile.avatarUrl || "";
    form.profileBio.value = profile.profileBio || "";
    const roles = getCurrentSession()?.roles || [profile.roleCode].filter(Boolean);
    text("profile-display-name", profile.realName || profile.username);
    text("profile-position", [profile.positionName, profile.department].filter(Boolean).join(" · ") || "未设置岗位");
    text("profile-bio-preview", profile.profileBio || "暂无个人简介");
    text("profile-phone-preview", profile.phone || "未填写");
    text("profile-email-preview", profile.email || "未填写");
    text("profile-user-id", profile.userId);
    text("profile-username", profile.username);
    text("profile-employee-no", profile.employeeNo || "未设置");
    text("profile-department", profile.department || "未设置");
    text("profile-position-name", profile.positionName || "未设置");
    text("profile-last-login", formatProfileTime(profile.lastLoginAt));
    document.getElementById("profile-role-list").innerHTML = roles.map(role => `<span>${escapeHtml(role)}</span>`).join("");
    renderProfileAvatar(profile.avatarUrl, profile.realName || profile.username);
    updateProfileBioCount();
}

function bindProfileEvents() {
    document.getElementById("refresh-profile")?.addEventListener("click", loadProfile);
    document.getElementById("profile-reset")?.addEventListener("click", () => renderProfile(profileSnapshot));
    document.getElementById("profile-avatar-input")?.addEventListener("input", event =>
        renderProfileAvatar(event.target.value.trim(), document.getElementById("profile-form").realName.value));
    document.querySelector("#profile-form [name='profileBio']")?.addEventListener("input", updateProfileBioCount);
    document.getElementById("profile-form")?.addEventListener("submit", async event => {
        event.preventDefault();
        try {
            profileSnapshot = await putJson("/profile", formToObject(event.target));
            renderProfile(profileSnapshot);
            const session = getCurrentSession();
            if (session) {
                session.user = { ...session.user, ...profileSnapshot };
                setCurrentSession(session);
                renderCurrentUser(session.user);
            }
            showMessage("个人资料已保存", "ok");
        } catch (error) {
            showMessage(error.message, "error");
        }
    });
}

function renderProfileAvatar(url, name) {
    const avatar = document.getElementById("profile-avatar");
    if (!avatar) return;
    avatar.textContent = "";
    avatar.style.backgroundImage = "";
    if (url && /^(https:\/\/|data:image\/)/i.test(url)) {
        avatar.style.backgroundImage = `url("${String(url).replaceAll('"', '%22')}")`;
        avatar.classList.add("has-image");
    } else {
        avatar.classList.remove("has-image");
        avatar.textContent = String(name || "U").trim().slice(0, 1).toUpperCase();
    }
}

function updateProfileBioCount() {
    const value = document.querySelector("#profile-form [name='profileBio']")?.value || "";
    text("profile-bio-count", value.length);
}

function formatProfileTime(value) {
    if (!value) return "暂无记录";
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? value : date.toLocaleString("zh-CN", { hour12: false });
}

function text(id, value) {
    const element = document.getElementById(id);
    if (element) element.textContent = value ?? "—";
}
