package org.example.beadando_ea;

import com.oanda.v20.Context;
import com.oanda.v20.account.AccountSummary;
import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.CandlestickGranularity;
import com.oanda.v20.instrument.InstrumentCandlesRequest;
import com.oanda.v20.instrument.InstrumentCandlesResponse;
import com.oanda.v20.trade.TradeCloseRequest;
import com.oanda.v20.trade.TradeSpecifier;
import com.oanda.v20.transaction.*;
import com.oanda.v20.pricing.ClientPrice;
import com.oanda.v20.pricing.PricingGetRequest;
import com.oanda.v20.pricing.PricingGetResponse;
import com.oanda.v20.primitives.InstrumentName;
import com.oanda.v20.order.OrderCreateRequest;
import com.oanda.v20.order.OrderCreateResponse;
import com.oanda.v20.order.MarketOrderRequest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
@Controller
public class BeadandoEaApplication {
    Context ctx = new Context(Config.URL, Config.TOKEN);

    public static void main(String[] args) {
        SpringApplication.run(BeadandoEaApplication.class, args);
    }

    @GetMapping("főoldal")
    public String főoldal(Model model) {
        return "főoldal";
    }

    @GetMapping("SOAP")
    public String SOAP(Model model) {
        return "SOAP";
    }

    @GetMapping("Faccount")
    public String faccount(Model model) {
        try {
            AccountSummary summary = ctx.account.summary(Config.ACCOUNTID).getAccount();
            Map<String, Object> accountMap = new LinkedHashMap<>();
            for (Field field : summary.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                accountMap.put(field.getName(), field.get(summary));
            }
            model.addAttribute("accountMap", accountMap);
        } catch (Exception e) {
            e.getMessage();
        }
        return "Faccount";
    }

    @GetMapping("/Factual")
    public String Factual(Model model) {
        model.addAttribute("par", new MessageActPrice());
        return "FormFactual";
    }

    @PostMapping("/Factual")
    public String factual(@ModelAttribute MessageActPrice messageActPrice, Model model) {
        Map<String, Object> priceMap = new LinkedHashMap<>(); // sorrendet tartja

        try {
            Context ctx = new Context(Config.URL, Config.TOKEN);
            List<String> instruments = List.of(messageActPrice.getInstrument());
            PricingGetRequest request = new PricingGetRequest(Config.ACCOUNTID, instruments);
            PricingGetResponse resp = ctx.pricing.get(request);

            for (ClientPrice price : resp.getPrices()) {
                priceMap.put("Instrument", price.getInstrument());
                priceMap.put("Time", price.getTime());
                priceMap.put("Status", price.getStatus());
                priceMap.put("Closeout Ask", price.getCloseoutAsk());
                priceMap.put("Closeout Bid", price.getCloseoutBid());
                priceMap.put("Ask", price.getAsks().get(0).getPrice());
                priceMap.put("Bid", price.getBids().get(0).getPrice());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        model.addAttribute("instr", messageActPrice.getInstrument());
        model.addAttribute("priceMap", priceMap);
        return "ResultFactual";
    }

    @GetMapping("/Fhist")
    public String Fhist(Model model) {
        model.addAttribute("par", new MessageHistPrice());
        return "FormFhist";
    }

    @PostMapping("/Fhist")
    public String fhist(@ModelAttribute MessageHistPrice messageHistPrice, Model model) {
        List<Map<String, Object>> candleList = new ArrayList<>();

        try {
            Context ctx = new Context(Config.URL, Config.TOKEN);

            InstrumentCandlesRequest request = new InstrumentCandlesRequest(
                    new InstrumentName(messageHistPrice.getInstrument())
            );

            switch (messageHistPrice.getGranularity()) {
                case "M1":
                    request.setGranularity(CandlestickGranularity.M1);
                    break;
                case "H1":
                    request.setGranularity(CandlestickGranularity.H1);
                    break;
                case "D":
                    request.setGranularity(CandlestickGranularity.D);
                    break;
                case "W":
                    request.setGranularity(CandlestickGranularity.W);
                    break;
                case "M":
                    request.setGranularity(CandlestickGranularity.M);
                    break;
                default:
                    request.setGranularity(CandlestickGranularity.H1);
            }

            request.setCount(10L);

            InstrumentCandlesResponse resp = ctx.instrument.candles(request);

            for (Candlestick candle : resp.getCandles()) {
                Map<String, Object> candleMap = new LinkedHashMap<>();
                candleMap.put("Time", candle.getTime());
                candleMap.put("Open", candle.getMid().getO());
                candleMap.put("High", candle.getMid().getH());
                candleMap.put("Low", candle.getMid().getL());
                candleMap.put("Close", candle.getMid().getC());
                candleList.add(candleMap);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Hiba a historikus adatok lekérésekor: " + e.getMessage());
        }

        model.addAttribute("instr", messageHistPrice.getInstrument());
        model.addAttribute("granularity", messageHistPrice.getGranularity());
        model.addAttribute("candles", candleList);

        return "ResultFhist";
    }

    @GetMapping("/Fnyit")
    public String fnyit(Model model) {
        model.addAttribute("param", new MessageOpenPosition());
        return "FormFnyit";
    }

    @PostMapping("/Fnyit")
    public String Fnyit(@ModelAttribute MessageOpenPosition messageOpenPosition, Model
            model) {
        String strOut;
        try {
            Context ctx = new Context(Config.URL,Config.TOKEN);
            InstrumentName instrument = new InstrumentName(messageOpenPosition.getInstrument());
            OrderCreateRequest request = new OrderCreateRequest(Config.ACCOUNTID);
            MarketOrderRequest marketorderrequest = new MarketOrderRequest();
            marketorderrequest.setInstrument(instrument);
            marketorderrequest.setUnits(messageOpenPosition.getUnits());
            request.setOrder(marketorderrequest);
            OrderCreateResponse response = ctx.order.create(request);
            strOut="tradeId: "+ response.getOrderFillTransaction().getId();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        model.addAttribute("instr", messageOpenPosition.getInstrument());
        model.addAttribute("units", messageOpenPosition.getUnits());
        model.addAttribute("id", strOut);
        return "ResultFnyit";
    }

    @GetMapping("Fzár")
    public String Fzár(Model model) {
        model.addAttribute("param", new MessageClosePosition());
        return "FormFzár";
    }
    @PostMapping("Fzár")
    public String fzár(@ModelAttribute MessageClosePosition messageClosePosition, Model model) {
        String tradeId= messageClosePosition.getTradeId()+"";
        String strOut="Closed tradeId= "+tradeId;
        try {
            Context ctx = new Context(Config.URL,Config.TOKEN);
            ctx.trade.close(new TradeCloseRequest(Config.ACCOUNTID, new TradeSpecifier(tradeId)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        model.addAttribute("tradeId", strOut);
        return "ResultFzár";
    }
}
