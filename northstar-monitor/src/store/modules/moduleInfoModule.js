
const moduleInfoModule = {
    state: () => ({
        list: []
    }),
    mutations: {
        updateList: (state, val) => {
            state.list = val
        }
    },
    actions: {},
    getters: {
        moduleList: (state) => {
            return state.list
        },
        module: (state) => (moduleName) => {
            return state.list.find(item => item.moduleName === moduleName)
        }
    }
}

export default moduleInfoModule