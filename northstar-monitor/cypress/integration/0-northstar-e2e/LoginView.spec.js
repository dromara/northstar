// LoginView.spec.js created with Cypress
//
// Start writing your Cypress tests below!
// If you're unfamiliar with how Cypress works,
// check out the link below and learn how to write your first test:
// https://on.cypress.io/writing-first-test
/* eslint-disable */
import packageJSON from '../../../package.json'

describe('登陆页-测试', () => {
    beforeEach(() => {
        cy.visit('http://localhost:8090')
    })

    describe('UI验证', () => {
        it('应该包括一个LOGO、及一个版本号', () => {
            cy.get('.logo').find('img').should('be.visible')
            cy.get('.logo').contains('v' + packageJSON.version)
        })
    
        it('应该有用户密码输入框、与登陆按钮', () => {
            cy.get('input').should('have.length', 2)
            cy.get('.el-form-item').contains('用户名')
            cy.get('.el-form-item').contains('密码')
            cy.get('button').contains('登陆')
        })
    })

    describe('行为验证', () => {
        it('用户名为空时，显示错误提示', () => {
            cy.get('.el-form-item').contains('用户名').parent().find('input').type('admin')
            cy.get('button').contains('登陆').click()
            cy.get('.el-message--error').contains('不能为空')
        })

        it('密码为空时，显示错误提示', () => {
            cy.get('.el-form-item').contains('密码').parent().find('input').type('123456')
            cy.get('button').contains('登陆').click()
            cy.get('.el-message--error').contains('不能为空')
        })

        it('用户名不正确时，显示错误提示', () => {
            cy.get('.el-form-item').contains('用户名').parent().find('input').type('admin2')
            cy.get('.el-form-item').contains('密码').parent().find('input').type('123456')
            cy.get('button').contains('登陆').click()
            cy.get('.el-message--error').contains('不正确')
        })

        it('密码不正确时，显示错误提示', () => {
            cy.get('.el-form-item').contains('用户名').parent().find('input').type('admin')
            cy.get('.el-form-item').contains('密码').parent().find('input').type('1234567')
            cy.get('button').contains('登陆').click()
            cy.get('.el-message--error').contains('不正确')
        })

        it('用户名与密码正确时，跳转页面', () => {
            cy.get('.el-form-item').contains('用户名').parent().find('input').type('admin')
            cy.get('.el-form-item').contains('密码').parent().find('input').type('123456')
            cy.get('button').contains('登陆').click()
            cy.url().should('not.contain', 'login')
        })
    })
})