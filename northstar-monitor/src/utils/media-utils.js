export default class MediaListener {
    constructor(onResize){
        if(typeof onResize !== 'function'){
            throw new TypeError('期望传入一个函数')
        }
        this.eventHandler = () => {
            onResize()
        }
        window.addEventListener('resize', this.eventHandler)
    }

    isMobile(){
        return window.matchMedia("(max-width: 767px)").matches 
    }

    destroy(){
        window.removeEventListener('resize', this.eventHandler)
    }
}