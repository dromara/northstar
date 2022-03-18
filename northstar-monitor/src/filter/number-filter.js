export default {
  accountingFormatter(val) {
    return val.toFixed(0).replace(/\d{1,3}(?=(\d{3})+(\.\d*)?$)/g, '$&,')
  }
}
