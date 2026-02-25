/**
 * WMS User Management Automation Testing
 * Test data được nạp từ: cypress/fixtures/userManagement.json
 */

describe('WMS User Management Automation Testing with Excel', () => {
    let td;
    let cfg;

    before(() => {
        cy.task('readExcel', 'cypress/fixtures/userManagement.xlsx').then((data) => {
            cfg = data.config;
            td  = data.testCases;
        });
    });

    beforeEach(() => {
        cy.visit(`${cfg.baseUrl}/login`);
        cy.get('#username').type(cfg.adminUser);
        cy.get('#password').type(cfg.adminPass);
        cy.get('button[type="submit"]').click();

        cy.visit(`${cfg.baseUrl}/admin/users`);
    });


    const fillUserForm = (username, fullName, password, role, warehouse) => {
        cy.get('a[href*="/admin/users/new"]').click();
        if (username)  cy.get('#username').type(username);
        if (fullName)  cy.get('#fullName').type(fullName);
        if (password)  cy.get('#password').type(password);
        if (role)      cy.get('#role').select(role);
        if (warehouse) cy.get('#warehouseId').select(warehouse);
        cy.get('button[type="submit"]').click();
    };

    const resolveField = (directValue, repeatConfigRaw) => {
            if (repeatConfigRaw) {
                const repeatConfig = typeof repeatConfigRaw === 'string'
                    ? JSON.parse(repeatConfigRaw)
                    : repeatConfigRaw;
                return repeatConfig.char.repeat(repeatConfig.times);
            }
            return directValue ?? '';
    };

    // ═════════════════════════════════════════════════════════════════════════
    //  TẠO USER MỚI
    // ═════════════════════════════════════════════════════════════════════════

    it('TD01: Tạo user không gắn kho', () => {
        const d = td.TD01;
        fillUserForm(d.username, d.fullName, d.password, d.role, d.warehouse);
        cy.get(d.expectedSelector).should('contain', d.expectedText);
    });

    it('TD02: Tạo user có gắn kho cụ thể', () => {
        const d = td.TD02;
        fillUserForm(d.username, d.fullName, d.password, d.role, d.warehouse);
        cy.get(d.expectedSelector).should('contain', d.expectedText);
    });

    it('TD03: Thất bại khi trống Username', () => {
        const d = td.TD03;
        fillUserForm(d.username, d.fullName, d.password, d.role, d.warehouse);
        cy.get(d.expectedSelector).should('have.class', d.expectedClass);
    });

    it('TD04: Thất bại khi trống Full Name', () => {
        const d = td.TD04;
        fillUserForm(d.username, d.fullName, d.password, d.role, d.warehouse);
        cy.get(d.expectedSelector).should('have.class', d.expectedClass);
    });

    it('TD05: Thất bại khi trống Role', () => {
        const d = td.TD05;
        fillUserForm(d.username, d.fullName, d.password, d.role, d.warehouse);
        cy.get(d.expectedSelector).should('have.class', d.expectedClass);
    });

    it('TD06: Password là bắt buộc khi tạo mới', () => {
        const d = td.TD06;
        fillUserForm(d.username, d.fullName, d.password, d.role, d.warehouse);
        cy.get(d.expectedSelector).should('have.class', d.expectedClass);
    });

    it('TD07: Username đã tồn tại (Duplicate)', () => {
        const d = td.TD07;
        fillUserForm(d.username, d.fullName, d.password, d.role, d.warehouse);
        cy.get(d.expectedSelector).should('contain', d.expectedText);
    });

    it('TD08 & TD09: Kiểm tra biên dưới (1 ký tự)', () => {
        const d = td.TD08_09;
        fillUserForm(d.username, d.fullName, d.password, d.role, d.warehouse);
        cy.get(d.expectedSelector).should('exist');
    });

    it('TD10 & TD11: Kiểm tra biên tối đa (Username 50, Full Name 100)', () => {
        const d       = td.TD10_11;
        const username = resolveField(d.username, d.usernameRepeat);
        const fullName = resolveField(d.fullName, d.fullNameRepeat);
        fillUserForm(username, fullName, d.password, d.role, d.warehouse);
        cy.get(d.expectedSelector).should('exist');
    });

    it('TD12: Vượt quá 50 ký tự Username', () => {
        const d        = td.TD12;
        const username = resolveField(d.username, d.usernameRepeat);
        fillUserForm(username, d.fullName, d.password, d.role, d.warehouse);
        cy.get(d.expectedSelector).should('contain', d.expectedText);
    });

    it('TD13: Vượt quá 100 ký tự Full Name', () => {
        const d        = td.TD13;
        const fullName = resolveField(d.fullName, d.fullNameRepeat);
        fillUserForm(d.username, fullName, d.password, d.role, d.warehouse);
        cy.get(d.expectedSelector).should('contain', d.expectedText);
    });

    // ═════════════════════════════════════════════════════════════════════════
    //  CẬP NHẬT USER
    // ═════════════════════════════════════════════════════════════════════════

    it('TD14: Cập nhật thông tin, giữ nguyên mật khẩu cũ', () => {
        const d = td.TD14;
        cy.contains('td', d.targetUsername).parent().find('a.btn-outline').click();
        cy.get('#fullName').clear().type(d.updatedFullName);
        cy.get('#password').should('be.empty');
        cy.get('button[type="submit"]').click();
        cy.get(d.expectedSelector).should('contain', d.expectedText);
    });

    it('TD15: Cập nhật mật khẩu mới', () => {
        const d = td.TD15;
        cy.contains('td', d.targetUsername).parent().find('a.btn-outline').click();
        cy.get('#password').type(d.newPassword);
        cy.get('button[type="submit"]').click();
        cy.get(d.expectedSelector).should('contain', d.expectedText);
    });

    it('TD16: Giữ nguyên Username của chính mình khi cập nhật', () => {
        const d = td.TD16;
        cy.contains('td', d.targetUsername).parent().find('a.btn-outline').click();
        cy.get('button[type="submit"]').click();
        cy.get(d.expectedSelector).should('contain', d.expectedText);
    });

    it('TD17: Đổi Username trùng với người khác', () => {
        const d = td.TD17;
        cy.contains('td', d.targetUsername).parent().find('a.btn-outline').click();
        cy.get('#username').clear().type(d.changeUsernameTo);
        cy.get('button[type="submit"]').click();
        cy.get(d.expectedSelector).should('contain', d.expectedText);
    });

    // ═════════════════════════════════════════════════════════════════════════
    //  TOGGLE TRẠNG THÁI & ĐĂNG NHẬP
    // ═════════════════════════════════════════════════════════════════════════

    it('TD18: Đảo trạng thái isActive (Toggle)', () => {
        const d = td.TD18;
        cy.contains('td', d.targetUsername).parent()
          .find('button').contains(d.toggleButtonText).click();
        cy.get(d.expectedSelector).should('contain', d.expectedText);
        cy.contains('td', d.targetUsername).parent()
          .find(d.expectedBadgeSelector).should('contain', d.expectedBadgeText);
    });

    it('TD19: Đăng nhập thất bại khi tài khoản bị Deactivate', () => {
        const d = td.TD19;
        cy.visit(`${cfg.baseUrl}/logout`);
        cy.visit(`${cfg.baseUrl}/login`);
        cy.get('#username').type(d.loginUsername);
        cy.get('#password').type(d.loginPassword);
        cy.get('button[type="submit"]').click();

        cy.url().should('include', d.expectedUrlFragment);
        cy.get(d.expectedSelector).should('contain', d.expectedText);
    });
    // ═══════════════════════════════════════════════════════════════
    //  BYPASS UI — TRUYỀN GIÁ TRỊ RÁC VÀO DROPDOWN QUA JAVASCRIPT
    // ═══════════════════════════════════════════════════════════════

    it('TD20: Role không hợp lệ khi bypass UI', () => {
        const d = td.TD20;
        cy.get('a[href*="/admin/users/new"]').click();

        cy.get('#username').type(d.username);
        cy.get('#fullName').type(d.fullName);
        cy.get('#password').type(d.password);
        cy.get('#warehouseId').select(d.warehouse);

        cy.get('#role').invoke('val', d.invalidRole);

        cy.get('button[type="submit"]').click();
        cy.get(d.expectedSelector).should('contain', d.expectedText);
    });

    it('TD21: Warehouse không hợp lệ khi bypass UI', () => {
        const d = td.TD21;
        cy.get('a[href*="/admin/users/new"]').click();

        cy.get('#username').type(d.username);
        cy.get('#fullName').type(d.fullName);
        cy.get('#password').type(d.password);
        cy.get('#role').select(d.role);

        cy.get('#warehouseId').invoke('val', d.invalidWarehouseId);

        cy.get('button[type="submit"]').click();
        cy.get(d.expectedSelector).should('contain', d.expectedText);
    });
});