///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS net.sourceforge.htmlunit:htmlunit:2.67.0
//DEPS ch.qos.reload4j:reload4j:1.2.19

//import org.apache.log4j.Logger;
//import org.apache.log4j.BasicConfigurator;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.DefaultCredentialsProvider;
import com.gargoylesoftware.htmlunit.javascript.host.event.Event;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.ScriptResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class archivefg {

    private WebClient client;
    private CookieManager cookieManager;

    public void initClient() {
        client = new WebClient(BrowserVersion.FIREFOX);
        client.getOptions().setJavaScriptEnabled(true);
        client.getOptions().setThrowExceptionOnScriptError(false);
        cookieManager = new CookieManager();
        cookieManager = client.getCookieManager();   
        cookieManager.setCookiesEnabled(true);
        client.setCookieManager(cookieManager);
    }

    public void loginForm() throws Exception {
        String username = "tcunning";
        String password = "3M8h5jeJrPGf";
        // Set some example credentials

        HtmlPage page = (HtmlPage) client.getPage("https://blogs.fangraphs.com/wp-login.php");
        client.waitForBackgroundJavaScript(50000); 

        HtmlDivision div = page.getHtmlElementById("login");
        HtmlForm form = page.getForms().get(0);
        form.getInputByName("log").setValueAttribute(username);  
        form.getInputByName("pwd").setValueAttribute(password);  

        page = form.getInputByValue("Sign In").click();
    }

    public void printCookies() {
        System.out.println("PRINTING COOKIES");
        Set<Cookie> cookies = cookieManager.getCookies();
        for (Cookie cook : cookies) {
            System.out.println("COOKIE " + cook.toString());
        }
        System.out.println("END PRINTING COOKIES");
    }

    public void postForm(String url, String fileName) throws Exception {
        HtmlPage page = client.getPage(url);
        client.waitForBackgroundJavaScript(50000); 
        System.out.println("------------------------------");

        List<HtmlAnchor> anchs = page.getByXPath("//a[contains(@class, 'data-export')]");
        for (HtmlAnchor anch : anchs) {
            System.out.println("anch : " + anch);
        }

        List<HtmlDivision> divs = page.getByXPath("//div[contains(@class, 'projections-data')]");
        for (HtmlDivision div : divs) {

            div.removeChild("div", 0);

            HtmlElement element = (HtmlElement) page.createElement("a");
            element.setAttribute("class", "data-export");
            element.setAttribute("href", "data:application/csv;charset=utf-8,undefined");
            element.setTextContent("Export Data");
            HtmlAnchor anchor = (HtmlAnchor) element;
            div.appendChild(anchor);

            System.out.println(div.asXml());

            Page p = anchor.click();

            File targetFile = new File(fileName);
            FileOutputStream fos = new FileOutputStream(targetFile);
            InputStream initialStream = p.getWebResponse().getContentAsStream();
            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            while ((bytesRead = initialStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            initialStream.close();
            fos.flush();
            fos.close();

            break;

        }
    }

    public String makeTR(String line) {
        StringBuffer sb = new StringBuffer();
        String[] tokens = line.split(",");
        if (tokens[tokens.length-1].startsWith("\"sa")) {
            sb.append("<tr>");
            for (String token : tokens) {
                token = token.replaceAll("\"", "");
                sb.append("<td>" + token + "</td>");
            }
            sb.append("</tr>");
        }
        return sb.toString();
    }

    public void filterForProspects(String filename) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line = null;

        String outputFileName = filename.replace(".csv", ".html").replace("steamer", "prospects");
        PrintWriter pw = new PrintWriter(new FileOutputStream(outputFileName), true);
        pw.println("<html>\n<table class=\"sortable\">");

        // We want the header line
        line = br.readLine();
        String[] tokens = line.split(",");
        StringBuffer sb = new StringBuffer();
        sb.append("<thead><tr>");
        for (String token : tokens) {
            token = token.replaceAll("\"", "");
            sb.append("<th>" + token + "</th>");
        }
        sb.append("</tr></thead>");
        pw.println(sb.toString());

        while ((line = br.readLine()) != null) {
            String tr = makeTR(line);
            if (!tr.isEmpty()) {
                pw.println(tr);
            }
        }
        pw.println("<link href=\"https://cdn.jsdelivr.net/gh/tofsjonas/sortable/sortable.min.css\" rel=\"stylesheet\" />");
        pw.println("<script src=\"https://cdn.jsdelivr.net/gh/tofsjonas/sortable/sortable.min.js\"></script>");
        pw.println("</table>\n</html>");
        pw.flush();
        pw.close();
    }

    public static void main(String... args) throws Exception {
        System.getProperties().put("org.apache.commons.logging.simplelog.defaultlog", "error");

        LocalDateTime ldt = LocalDateTime.now().plusDays(1);
        DateTimeFormatter formmat1 = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ENGLISH);
        String formatter = formmat1.format(ldt);
        File directory = new File(formatter);
        directory.mkdirs();

        String offense = directory + File.separator + "steameroffense.csv";
        String pitching = directory + File.separator + "steamerpitching.csv";

        archivefg fg = new archivefg();

        fg.initClient();
        fg.printCookies();
        fg.loginForm();
        fg.printCookies();

        // Check whether file exists already
        File offFile = new File(offense);
        if (!offFile.exists()) {
            fg.postForm("https://www.fangraphs.com/projections?pos=all&stats=bat&type=steamer", offense);
        }

        /* 
        File pitchingFile = new File(pitching);
        if (!pitchingFile.exists()) {
            fg.postForm("https://www.fangraphs.com/projections.aspx?pos=all&stats=pit&type=steamer&team=0&lg=all&players=0", pitching);
        }
        fg.filterForProspects(offense);
        fg.filterForProspects(pitching);


        */
    }
}