<template>
  <div class="ns-price-board">
    <div class="ns-price-board__row" role="ask5">
      <div class="ns-price-board__cell ns-price-board__cell--left"></div>
      <div class="ns-price-board__cell ns-price-board__cell--mid">
        {{ askPrice4 }}
      </div>
      <div class="ns-price-board__cell ns-price-board__cell--right">
        {{ askVol[4] }}
      </div>
    </div>
    <div class="ns-price-board__row" role="ask4">
      <div class="ns-price-board__cell ns-price-board__cell--left"></div>
      <div class="ns-price-board__cell ns-price-board__cell--mid">
        {{ askPrice3 }}
      </div>
      <div class="ns-price-board__cell ns-price-board__cell--right">
        {{ askVol[3] }}
      </div>
    </div>
    <div class="ns-price-board__row" role="ask3">
      <div class="ns-price-board__cell ns-price-board__cell--left"></div>
      <div class="ns-price-board__cell ns-price-board__cell--mid">
        {{ askPrice2 }}
      </div>
      <div class="ns-price-board__cell ns-price-board__cell--right">
        {{ askVol[2] }}
      </div>
    </div>
    <div class="ns-price-board__row" role="ask2">
      <div class="ns-price-board__cell ns-price-board__cell--left"></div>
      <div class="ns-price-board__cell ns-price-board__cell--mid">
        {{ askPrice1 }}
      </div>
      <div class="ns-price-board__cell ns-price-board__cell--right">
        {{ askVol[1] }}
      </div>
    </div>
    <div class="ns-price-board__row" role="ask1">
      <div class="ns-price-board__cell ns-price-board__cell--left"></div>
      <div class="ns-price-board__cell ns-price-board__cell--mid">
        {{ askPrice0 }}
      </div>
      <div class="ns-price-board__cell ns-price-board__cell--right">
        {{ askVol[0] }}
      </div>
    </div>
    <div class="ns-price-board__row ns-price-board__row--highlight" role="latest">
      <div class="ns-price-board__cell ns-price-board__cell--left">
        {{ volume }}
      </div>
      <div class="ns-price-board__cell ns-price-board__cell--mid">
        {{ lastPrice.toFixed(precision) }}
      </div>
      <div class="ns-price-board__cell ns-price-board__cell--right">
        {{ volume }}
      </div>
    </div>
    <div class="ns-price-board__row" role="bid1">
      <div class="ns-price-board__cell ns-price-board__cell--left">
        {{ bidVol[0] }}
      </div>
      <div class="ns-price-board__cell ns-price-board__cell--mid">
        {{ bidPrice0 }}
      </div>
      <div class="ns-price-board__cell ns-price-board__cell--right"></div>
    </div>
    <div class="ns-price-board__row" role="bid2">
      <div class="ns-price-board__cell ns-price-board__cell--left">
        {{ bidVol[1] }}
      </div>
      <div class="ns-price-board__cell ns-price-board__cell--mid">
        {{ bidPrice1 }}
      </div>
      <div class="ns-price-board__cell ns-price-board__cell--right"></div>
    </div>
    <div class="ns-price-board__row" role="bid3">
      <div class="ns-price-board__cell ns-price-board__cell--left">
        {{ bidVol[2] }}
      </div>
      <div class="ns-price-board__cell ns-price-board__cell--mid">
        {{ bidPrice2 }}
      </div>
      <div class="ns-price-board__cell ns-price-board__cell--right"></div>
    </div>
    <div class="ns-price-board__row" role="bid4">
      <div class="ns-price-board__cell ns-price-board__cell--left">
        {{ bidVol[3] }}
      </div>
      <div class="ns-price-board__cell ns-price-board__cell--mid">
        {{ bidPrice3 }}
      </div>
      <div class="ns-price-board__cell ns-price-board__cell--right"></div>
    </div>
    <div class="ns-price-board__row" role="bid5">
      <div class="ns-price-board__cell ns-price-board__cell--left">
        {{ bidVol[4] }}
      </div>
      <div class="ns-price-board__cell ns-price-board__cell--mid">
        {{ bidPrice4 }}
      </div>
      <div class="ns-price-board__cell ns-price-board__cell--right"></div>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    tick: {
      type: Object,
      default: () => {}
    },
    precision: {
      type: Number,
      default: 0
    }
  },
  data() {
    return {
      askPrice: [0, 0, 0, 0, 0],
      askVol: [0, 0, 0, 0, 0],
      bidPrice: [0, 0, 0, 0, 0],
      bidVol: [0, 0, 0, 0, 0],
      lastPrice: 0,
      volume: 0
    }
  },
  watch: {
    tick: function (tick) {
      this.askVol = tick.askvolumeList
      this.askPrice = tick.askpriceList
      this.bidPrice = tick.bidpriceList
      this.bidVol = tick.bidvolumeList
      this.lastPrice = tick.lastprice
      this.volume = tick.volumedelta
    }
  },
  computed: {
    askPrice4(){
      return this.askPrice[4].toFixed(this.precision)
    },
    askPrice3(){
      return this.askPrice[3].toFixed(this.precision)
    },
    askPrice2(){
      return this.askPrice[2].toFixed(this.precision)
    },
    askPrice1(){
      return this.askPrice[1].toFixed(this.precision)
    },
    askPrice0(){
      return this.askPrice[0].toFixed(this.precision)
    },
    bidPrice0(){
      return this.bidPrice[0].toFixed(this.precision)
    },
    bidPrice1(){
      return this.bidPrice[1].toFixed(this.precision)
    },
    bidPrice2(){
      return this.bidPrice[2].toFixed(this.precision)
    },
    bidPrice3(){
      return this.bidPrice[3].toFixed(this.precision)
    },
    bidPrice4(){
      return this.bidPrice[4].toFixed(this.precision)
    }
  }
}
</script>

<style>
.ns-price-board {
  font-size: 8px;
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: rgba(20, 20, 20, 0.4);
  color: lightgray;
}
.ns-price-board__row {
  display: flex;
  width: 100%;
  height: 100%;
}
.ns-price-board__cell {
  width: 100%;
  height: 100%;
  text-align: center;
}
.ns-price-board__row--highlight {
  border-top: 1px solid rgb(206, 48, 48);
  border-bottom: 1px solid rgb(206, 48, 48);
  font-weight: bolder;
  font-size: 14px;
}
</style>
