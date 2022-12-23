// LoginView.spec.js created with Cypress
//
// Start writing your Cypress tests below!
// If you're unfamiliar with how Cypress works,
// check out the link below and learn how to write your first test:
// https://on.cypress.io/writing-first-test
/* eslint-disable */
describe('登陆页-测试', () => {
    beforeEach(() => {
        cy.visit('https://localhost:8090/#/')
    })

    it('用户名为空时，显示错误提示', () => {
        cy.contains('密码').parent().find('input').type('123456')
        cy.contains('登陆').click()
        cy.get('.el-message--error').contains('不能为空')
    })
    
    it('密码为空时，显示错误提示', () => {
        cy.contains('用户名').parent().find('input').type('admin')
        cy.contains('登陆').click()
        cy.get('.el-message--error').contains('不正确')
    })

    it('用户名不正确时，显示错误提示', () => {
        cy.contains('用户名').parent().find('input').type('admin2')
        cy.contains('密码').parent().find('input').type('123456')
        cy.contains('登陆').click()
        cy.get('.el-message--error').contains('不正确')
    })

    it('密码不正确时，显示错误提示', () => {
        cy.contains('用户名').parent().find('input').type('admin')
        cy.contains('密码').parent().find('input').type('1234567')
        cy.contains('登陆').click()
        cy.get('.el-message--error').contains('不正确')
    })

    it('用户名与密码正确时，跳转页面', () => {
        cy.contains('用户名').parent().find('input').type('admin')
        cy.contains('密码').parent().find('input').type('123456')
        cy.contains('登陆').click()
        cy.url().should('not.contain', 'login')
    })
})