const MediaListener = {

    isMobile: () => {
        return window.matchMedia("(max-width: 767px)").matches        
    },

    onResize: () => {
        console.warn('未定义屏幕调整时的回调')
    }
}

window.addEventListener('resize', () => {
    MediaListener.onResize()
})

export default MediaListener