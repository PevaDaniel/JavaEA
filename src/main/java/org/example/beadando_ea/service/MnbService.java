package org.example.beadando_ea.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class MnbService {

    private static final String URL = "http://www.mnb.hu/arfolyamok.asmx";
    private static final String SOAP_ACTION = "http://www.mnb.hu/webservices/GetExchangeRates";

    public static class RatePoint {
        public LocalDate date;
        public BigDecimal value;
        public RatePoint(LocalDate d, BigDecimal v) {
            this.date = d;
            this.value = v;
        }
    }

    public List<RatePoint> getExchangeRates(String start, String end, String currency) throws Exception {
        String envelope = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:mnb="http://www.mnb.hu/webservices/">
                  <soapenv:Header/>
                  <soapenv:Body>
                    <mnb:GetExchangeRates>
                      <mnb:startDate>%s</mnb:startDate>
                      <mnb:endDate>%s</mnb:endDate>
                      <mnb:currencyNames>%s</mnb:currencyNames>
                    </mnb:GetExchangeRates>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(start, end, currency);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        headers.set("SOAPAction", SOAP_ACTION);

        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> resp = rt.postForEntity(URL, new HttpEntity<>(envelope, headers), String.class);

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("SOAP hívás sikertelen: " + resp.getStatusCode());
        }

        return parseRates(resp.getBody(), currency);
    }

    private List<RatePoint> parseRates(String soapXml, String currency) throws Exception {
        // 💡 Első lépés: az escape-elt XML visszaalakítása igazi XML-é
        soapXml = soapXml
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&amp;", "&");

        int start = soapXml.indexOf("<MNBExchangeRates");
        int end = soapXml.indexOf("</MNBExchangeRates>");
        if (start < 0 || end < 0) {
            System.out.println("❌ Nem található <MNBExchangeRates> blokk az XML-ben!");
            System.out.println("SOAP válasz (rövidítve): " + soapXml.substring(0, Math.min(500, soapXml.length())));
            return List.of();
        }

        String inner = soapXml.substring(start, end + "</MNBExchangeRates>".length());

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        Document doc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(inner)));

        NodeList dayNodes = doc.getElementsByTagName("Day");
        List<RatePoint> points = new ArrayList<>();

        for (int i = 0; i < dayNodes.getLength(); i++) {
            Element day = (Element) dayNodes.item(i);
            String dateStr = day.getAttribute("date").replace('.', '-');

            LocalDate date;
            if (dateStr.contains(".")) {
                String[] parts = dateStr.split("\\.");
                date = LocalDate.of(
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2])
                );
            } else {
                date = LocalDate.parse(dateStr);
            }

            NodeList rates = day.getElementsByTagName("Rate");
            for (int j = 0; j < rates.getLength(); j++) {
                Element rateEl = (Element) rates.item(j);
                if (!currency.equalsIgnoreCase(rateEl.getAttribute("curr"))) continue;

                String valStr = rateEl.getTextContent().trim().replace(',', '.');
                if (valStr.isEmpty() || valStr.equalsIgnoreCase("n.a.")) continue;

                points.add(new RatePoint(date, new BigDecimal(valStr)));
            }
        }

        System.out.println("✅ " + points.size() + " árfolyam sikeresen feldolgozva.");
        return points;
    }


}
