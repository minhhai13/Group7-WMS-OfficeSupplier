describe('WMS User Management Automation Testing', () => {
    const adminUser = 'admin@gmail.com';
    const adminPass = '123';
    const baseUrl = 'http://localhost:8080';

    beforeEach(() => {
        cy.visit(`${baseUrl}/login`);
        cy.get('#username').type(adminUser);
        cy.get('#password').type(adminPass);
        cy.get('button[type="submit"]').click();

        cy.visit(`${baseUrl}/admin/users`);
    });

    const fillUserForm = (username, fullName, password, role, warehouse) => {
        cy.get('a[href*="/admin/users/new"]').click();
        if (username) cy.get('#username').type(username);
        if (fullName) cy.get('#fullName').type(fullName);
        if (password) cy.get('#password').type(password);
        if (role) cy.get('#role').select(role);
        if (warehouse) cy.get('#warehouseId').select(warehouse);
        cy.get('button[type="submit"]').click();
    };


    it('TD01: Tạo user không gắn kho', () => {
        fillUserForm('admin.hcm', 'Nguyen Van Admin', 'P@ss123', 'System Admin', '-- None --');
        cy.get('.alert-success').should('contain', 'User saved successfully'); //
    });

    it('TD02: Tạo user có gắn kho cụ thể', () => {
        fillUserForm('keeper01', 'Tran Thi Kho', '123456', 'Storekeeper', 'Ha Noi Warehouse 1');
        cy.get('.alert-success').should('contain', 'User saved successfully');
    });

    it('TD03: Thất bại khi trống Username', () => {
        fillUserForm('', 'Nguyen Van A', '123456', 'Storekeeper', 'Ha Noi Warehouse 1');
        cy.get('#username').should('have.class', 'is-invalid');
    });

    it('TD04: Thất bại khi trống Full Name', () => {
        fillUserForm('user01', '', '123456', 'Storekeeper', 'Ha Noi Warehouse 1');
        cy.get('#fullName').should('have.class', 'is-invalid');
    });

    it('TD05: Thất bại khi trống Role', () => {
        fillUserForm('user01', 'Nguyen Van A', '123456', '', 'Ha Noi Warehouse 1');
        cy.get('#role').should('have.class', 'is-invalid');
    });

    it('TD06: Password là bắt buộc khi tạo mới', () => {
        fillUserForm('user01', 'Nguyen Van A', '', 'Storekeeper', 'Ha Noi Warehouse 1');
        cy.get('#password').should('have.class', 'is-invalid');
    });

    it('TD07: Username đã tồn tại (Duplicate)', () => {
        // Thử tạo lại username đã tạo ở TD01
        fillUserForm('admin.hcm', 'Trung Ten', '123456', 'Warehouse Admin', 'Ha Noi Warehouse 1');
        cy.get('.invalid-feedback').should('contain', 'Username already exists');
    });

    it('TD08 & TD09: Kiểm tra biên dưới (1 ký tự)', () => {
        fillUserForm('a', 'X', '123456', 'Storekeeper', 'Ha Noi Warehouse 1');
        cy.get('.alert-success').should('exist');
    });

    it('TD10 & TD11: Kiểm tra biên tối đa (Username 50, Full Name 100)', () => {
        const longUser = 'a'.repeat(50);
        const longName = 'b'.repeat(100);
        fillUserForm(longUser, longName, '123456', 'Storekeeper', 'Ha Noi Warehouse 1');
        cy.get('.alert-success').should('exist');
    });

    it('TD12: Vượt quá 50 ký tự Username', () => {
        const overLongUser = 'a'.repeat(51);
        fillUserForm(overLongUser, 'Nguyen Van B', '123456', 'Storekeeper', 'Ha Noi Warehouse 1');
        cy.get('.invalid-feedback').should('contain', 'Username must be less than 50 characters'); //
    });

    it('TD13: Vượt quá 100 ký tự Full Name', () => {
        const overLongName = 'b'.repeat(101);
        fillUserForm('user02', overLongName, '123456', 'Storekeeper', 'Ha Noi Warehouse 1');
        cy.get('.invalid-feedback').should('contain', 'Full name must be less than 100 characters'); //
    });


    it('TD14: Cập nhật thông tin, giữ nguyên mật khẩu cũ', () => {
        cy.contains('td', 'admin.hcm').parent().find('a.btn-outline').click();
        cy.get('#fullName').clear().type('Nguyen Admin Update');
        cy.get('#password').should('be.empty');
        cy.get('button[type="submit"]').click();
        cy.get('.alert-success').should('contain', 'User saved successfully');
    });

    it('TD15: Cập nhật mật khẩu mới', () => {
        cy.contains('td', 'admin.hcm').parent().find('a.btn-outline').click();
        cy.get('#password').type('NewPass123');
        cy.get('button[type="submit"]').click();
        cy.get('.alert-success').should('contain', 'User saved successfully');
    });

    it('TD16: Giữ nguyên Username của chính mình khi cập nhật', () => {
        cy.contains('td', 'admin.hcm').parent().find('a.btn-outline').click();
        cy.get('button[type="submit"]').click();
        cy.get('.alert-success').should('contain', 'User saved successfully');
    });

    it('TD17: Đổi Username trùng với người khác', () => {
        cy.contains('td', 'keeper01').parent().find('a.btn-outline').click();
        cy.get('#username').clear().type('admin.hcm');
        cy.get('button[type="submit"]').click();
        cy.get('.invalid-feedback').should('contain', 'Username already exists');
    });


    it('TD18: Đảo trạng thái isActive (Toggle)', () => {
        cy.contains('td', 'admin.hcm').parent()
          .find('button').contains('Deactivate').click();
        cy.get('.alert-success').should('contain', 'User status updated');
        cy.contains('td', 'admin.hcm').parent().find('.badge-danger').should('contain', 'Inactive');
    });

    it('TD19: Đăng nhập thất bại khi tài khoản bị Deactivate', () => {
        cy.visit(`${baseUrl}/logout`);
        cy.visit(`${baseUrl}/login`);
        cy.get('#username').type('admin.hcm');
        cy.get('#password').type('NewPass123');
        cy.get('button[type="submit"]').click();

        cy.url().should('include', '/login');
        cy.get('.alert-danger').should('contain', 'Invalid username or password');
    });
});