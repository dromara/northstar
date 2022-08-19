export default {
  accountingFormatter(val) {
    return val.toFixed(0).replace(/\d{1,3}(?=(\d{3})+(\.\d*)?$)/g, '$&,')
  },
  smartFormatter(val){
    if(val === 0 || val === '0'){
      return val
    }
    if(parseInt(val) === val){
      return val
    }
    const number = parseFloat(val)
    if(isNaN(number)){
      return val
    }
    return number.toFixed(2)
    
  }
}
