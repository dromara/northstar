// GatewayMgmt.spec.js created with Cypress
//
// Start writing your Cypress tests below!
// If you're unfamiliar with how Cypress works,
// check out the link below and learn how to write your first test:
// https://on.cypress.io/writing-first-test
/* eslint-disable */

describe('网关管理-测试', () => {
    before(() => {
        cy.visit('http://localhost:8090')
        cy.contains('用户名').parent().find('input').type('admin')
        cy.contains('密码').parent().find('input').type('123456')
        cy.contains('登陆').click()
        cy.wait(500)
        cy.visit('http://localhost:8090/#/mktgateway')
        cy.wait(300)
    })

    describe('行情网关管理-基础测试', () => {
        beforeEach(() => {
            cy.Cookies.preserveOnce('JSESSIONID')
        })
    
        it('应该可以新增SIM行情网关', () => {
            cy.intercept('POST','/northstar/gateway').as('createGateway')
            cy.contains('新建').click()
            cy.get('.el-dialog').contains('网关类型').parent().find('.el-select').click()
            cy.get('.el-select-dropdown').contains('SIM').click()
            cy.get('.el-dialog').contains('订阅合约').parent().find('.el-select').click()
            cy.get('.el-select-dropdown').contains('模拟合约').click()
            cy.get('.el-dialog').filter(':visible').find('button').last().click()
            cy.wait('@createGateway').should('have.nested.property', 'response.statusCode', 200)

            cy.get('.el-table__row').should('have.length', 1)
        })

        it('应该可以连线与断开网关，连线后修改、删除按钮不可用', () => {
            cy.get('.el-table__row').first().contains('连线').click()
            cy.get('.el-table__row').first().contains('已连接')
            cy.get('.el-table__row').first().contains('修改').should('be.disabled')
            cy.get('.el-table__row').first().contains('删除').should('be.disabled')
            cy.get('.el-table__row').first().contains('断开').click()
            cy.get('.el-table__row').first().contains('已断开')
            cy.get('.el-table__row').first().contains('修改').should('be.enabled')
            cy.get('.el-table__row').first().contains('删除').should('be.enabled')
        })

        it('应该可以删除SIM行情网关', () => {
            cy.intercept('DELETE','/northstar/gateway?gatewayId=SIM').as('delGateway')
            cy.get('.el-table__row').first().contains('删除').click()
            cy.get('.el-popconfirm').find('button').contains('确定').click()
            cy.wait('@delGateway').should('have.nested.property', 'response.statusCode', 200)

            cy.get('.el-table__row').should('have.length', 0)
        })

        after(() => {
            cy.request('http://localhost/resetDB')
        })
    })
    
    describe('账户网关管理-基础测试', () => {
        before(() => {
            cy.contains('新建').click()
            cy.get('.el-dialog').contains('网关类型').parent().find('.el-select').click()
            cy.get('.el-select-dropdown').contains('SIM').click()
            cy.get('.el-dialog').contains('订阅合约').parent().find('.el-select').click()
            cy.get('.el-select-dropdown').contains('模拟合约').click()
            cy.get('.el-dialog').filter(':visible').find('button').last().click()
            cy.visit('http://localhost:8090/#/tdgateway')
            cy.wait(300)
        })
        beforeEach(() => {
            cy.Cookies.preserveOnce('JSESSIONID')
        })
    
        it('应该可以新增一个SIM账户', () => {
            cy.intercept('POST','/northstar/gateway').as('createGateway')
            cy.get('button').contains('新建').click()
            cy.get('.el-dialog').contains('账户ID').parent().find('input').type('testAccount')
            cy.get('.el-dialog').contains('账户类型').parent().find('.el-select').click()
            cy.get('.el-select-dropdown').filter(':visible').contains('SIM').click()
            cy.get('.el-dialog').contains('行情网关').parent().find('.el-select').click()
            cy.get('.el-select-dropdown').filter(':visible').last().contains('SIM').click()
            cy.get('.el-dialog').filter(':visible').find('button').last().click()
            cy.wait('@createGateway').should('have.nested.property', 'response.statusCode', 200)

            cy.get('.el-table__row').should('have.length', 1)
        })
    
        it('应该可以连线、断开网关，连线后修改、删除按钮不可用', () => {
            cy.get('.el-table__row').first().contains('连线').click()
            cy.get('.el-table__row').first().contains('已连接')
            cy.get('.el-table__row').first().contains('修改').should('be.disabled')
            cy.get('.el-table__row').first().contains('删除').should('be.disabled')
            cy.get('.el-table__row').first().contains('断开').click()
            cy.get('.el-table__row').first().contains('已断开')
            cy.get('.el-table__row').first().contains('修改').should('be.enabled')
            cy.get('.el-table__row').first().contains('删除').should('be.enabled')
        })

        it('应该可以删除SIM账户', () => {
            cy.intercept('DELETE','/northstar/gateway?gatewayId=testAccount').as('delGateway')
            cy.get('.el-table__row').first().contains('删除').click()
            cy.get('.el-popconfirm').find('button').contains('确定').click()
            cy.wait('@delGateway').should('have.nested.property', 'response.statusCode', 200)

            cy.get('.el-table__row').should('have.length', 0)
        })

        after(() => {
            cy.request('http://localhost/resetDB')
        })
    })

    describe('网关管理-级联测试', () => {
        beforeEach(() => {
            cy.Cookies.preserveOnce('JSESSIONID')
            cy.visit('http://localhost:8090/#/mktgateway')
            cy.contains('新建').click()
            cy.get('.el-dialog').contains('网关类型').parent().find('.el-select').click()
            cy.get('.el-select-dropdown').contains('SIM').click()
            cy.get('.el-dialog').contains('订阅合约').parent().find('.el-select').click()
            cy.get('.el-select-dropdown').contains('模拟合约').click()
            cy.get('.el-dialog').filter(':visible').find('button').last().click()
            cy.visit('http://localhost:8090/#/tdgateway')
            cy.wait(300)
            cy.get('button').contains('新建').click()
            cy.get('.el-dialog').contains('账户ID').parent().find('input').type('testAccount')
            cy.get('.el-dialog').contains('账户类型').parent().find('.el-select').click()
            cy.get('.el-select-dropdown').filter(':visible').contains('SIM').click()
            cy.get('.el-dialog').contains('行情网关').parent().find('.el-select').click()
            cy.get('.el-select-dropdown').filter(':visible').last().contains('SIM').click()
            cy.get('.el-dialog').filter(':visible').find('button').last().click()
            cy.visit('http://localhost:8090/#/mktgateway')
            cy.wait(300)
        })
        it('当行情网关被账户网关绑定时，行情网关不能被删除', () => {
            cy.get('.el-table__row').first().contains('删除').click()
            cy.get('.el-popconfirm').find('button').contains('确定').click()

            cy.get('.el-message--error').contains('请先解除绑定')
        })
        after(() => {
            cy.request('DELETE', 'http://localhost/northstar/gateway?gatewayId=testAccount')
            cy.request('http://localhost/resetDB')
        })
    })
})

