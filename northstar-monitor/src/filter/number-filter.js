

export default {
  accountingFormatter(val) {
    return val.toFixed(0).replace(/\d{1,3}(?=(\d{3})+(\.\d*)?$)/g, '$&,')
  },
  smartFormatter(val){
    if(val === 0 || val === '0' || isNaN(val)){
      return val
    }
    let precision
    for(precision = 0; precision<6; precision++) {
      const number = val * Math.pow(10, precision)
      if(parseInt(number) - number === 0){
        break;
      }
    }
    return val.toFixed(precision)
  }
}
