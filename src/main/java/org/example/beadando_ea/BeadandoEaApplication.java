package org.example.beadando_ea;

import com.oanda.v20.Context;
import com.oanda.v20.account.AccountSummary;
import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.CandlestickGranularity;
import com.oanda.v20.instrument.InstrumentCandlesRequest;
import com.oanda.v20.instrument.InstrumentCandlesResponse;
import com.oanda.v20.pricing.ClientPrice;
import com.oanda.v20.pricing.PricingGetRequest;
import com.oanda.v20.pricing.PricingGetResponse;
import com.oanda.v20.primitives.InstrumentName;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Field;
import java.util.*;

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
    public String faccount( Model model) {
        try {
            AccountSummary summary = ctx.account.summary(Config.ACCOUNTID).getAccount();
            Map<String,Object> accountMap = new LinkedHashMap<>();
            for(Field field : summary.getClass().getDeclaredFields()) {
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
        String strOut="";
        List<String> instruments = new ArrayList<>( );
        instruments.add(messageActPrice.getInstrument());
        try {
            Context ctx = new Context(Config.URL,Config.TOKEN);
            PricingGetRequest request = new PricingGetRequest(Config.ACCOUNTID, instruments);
            PricingGetResponse resp = ctx.pricing.get(request);
            for (ClientPrice price : resp.getPrices())
                strOut+=price+"<br>";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        model.addAttribute("instr", messageActPrice.getInstrument());
        model.addAttribute("price", strOut);
        return "ResultFactual";
    }
    @GetMapping("Fhist")
    public String Fhist(Model model) {
        model.addAttribute("param", new MessageHistPrice());
        return "FormFhist";
    }
    @PostMapping("/Fhist")
    public String fhist(@ModelAttribute MessageHistPrice messageHistPrice, Model model) {
        String strOut;
        try {
            InstrumentCandlesRequest request = new InstrumentCandlesRequest(new InstrumentName(messageHistPrice.getInstrument()));
            switch (messageHistPrice.getGranularity()) {
                case "M1": request.setGranularity(CandlestickGranularity.M1); break;
                case "H1": request.setGranularity(CandlestickGranularity.H1); break;
                case "D":  request.setGranularity(CandlestickGranularity.D); break;
                case "W":  request.setGranularity(CandlestickGranularity.W); break;
                case "M":  request.setGranularity(CandlestickGranularity.M); break;
            }
            Context ctx = new Context(Config.URL,Config.TOKEN);
            request.setCount(Long.valueOf(10)); // utolsó 10 árat kérjünk
            InstrumentCandlesResponse resp = ctx.instrument.candles(request);
            strOut = "";
            for (Candlestick candle : resp.getCandles())
                strOut += candle.getTime() + "\t" + candle.getMid().getC() + ";";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        model.addAttribute("instr", messageHistPrice.getInstrument());
        model.addAttribute("granularity", messageHistPrice.getGranularity());
        model.addAttribute("price", strOut);
        return "ResultFhist";
    }
    @GetMapping("Fnyit")
    public String fnyit(Model model) {
        return "Fnyit";
    }
    @GetMapping("Fzár")
    public String Fzár(Model model) {
        return "Fzár";
    }

}
