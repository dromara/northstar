

export default {
  accountingFormatter(val) {
    return val.toFixed(0).replace(/\d{1,3}(?=(\d{3})+(\.\d*)?$)/g, '$&,')
  },
  smartFormatter(val){
    if(val === 0 || val === '0' || isNaN(val) || typeof val !== 'number'){
      return val
    }
    let precision
    for(precision = 0; precision<10; precision++) {
      const number = val * Math.pow(10, precision)
      if(parseInt(number) - number === 0){
        break;
      }
    }
    if(precision < 4){
      return val.toFixed(precision)
    }
    return val.toExponential(3)
  }
}
