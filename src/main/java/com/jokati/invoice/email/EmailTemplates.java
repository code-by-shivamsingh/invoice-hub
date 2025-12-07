
package com.jokati.invoice.email;

import org.springframework.web.util.HtmlUtils;

/**
 * Centralized HTML email templates for the Jokati application.
 * Uses Java 17 Text Blocks for clean multi-line HTML strings.
 */
public final class EmailTemplates {

    public EmailTemplates() {
        // utility class
    }
    

   public String angebotErhaltenTemplate(String carrierCompanyName) {
        return """
               <html>
                 <body>
                   <p>Sehr geehrte Damen und Herren,</p>
                   <p>Sie haben ein neues Angebot von <strong>%s</strong> für Ihre Frachtausschreibung erhalten.</p>
                   <p>Mit freundlichen Grüßen,<br/>Jokati Team</p>
                 </body>
               </html>
               """.formatted(carrierCompanyName == null ? "Ihr Carrier" : carrierCompanyName);
    }


    /**
     * Angebot erhalten – template shown to the shipper when an offer was received.
     *
     * @param carrierCompany Company name to display in bold within the email body.
     * @return HTML string ready to be sent via MailService.
     */
    public static String angebotErhalten(String carrierCompany) {
        // Escape the company name to prevent HTML injection
        String safeCompany = HtmlUtils.htmlEscape(carrierCompany == null ? "" : carrierCompany);

        return """
            <html xmlns="http://www.w3.org/1999/xhtml">
              <head>
                <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
                <meta http-equiv="X-UA-Compatible" content="IE=edge" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <title>Jokati Ausschreibung</title>
                <style type="text/css">
                  body {
                    background-color: #ffffff;
                    color: #4e5d78;
                    margin: 0;
                  }

                  table {
                    border-spacing: 0;
                    width: 100%;
                  }

                  td {
                    padding: 0;
                  }

                  p {
                    margin: 0;
                  }

                  .wrapper {
                    background-color: #fafbfc;
                    table-layout: fixed;
                    width: 100%;
                  }

                  .main {
                    background-color: #ffffff;
                    border-spacing: 0;
                    font-family: Tahoma, sans-serif;
                    margin: 0 auto;
                    max-width: 600px;
                    width: 100%;
                  }

                  .container {
                    padding: 0 16px;
                  }

                  .border {
                    height: 8px;
                    background-color: #287fb8;
                  }

                  .border--top {
                    border-bottom-left-radius: 6px;
                    border-bottom-right-radius: 6px;
                  }

                  .border--bottom {
                    border-top-left-radius: 6px;
                    border-top-right-radius: 6px;
                  }

                  .separator {
                    background-color: #dee7eb;
                    height: 1px;
                  }

                  .brand {
                    background: rgb(40, 127, 184);
                    background: -moz-linear-gradient(90deg, rgba(40, 127, 184, 1) 0%, rgba(43, 61, 79, 1) 100%);
                    background: -webkit-linear-gradient(90deg, rgba(40, 127, 184, 1) 0%, rgba(43, 61, 79, 1) 100%);
                    background: linear-gradient(90deg, rgba(40, 127, 184, 1) 0%, rgba(43, 61, 79, 1) 100%);
                    filter: progid:DXImageTransform.Microsoft.gradient(startColorstr="#287fb8",endColorstr="#2b3d4f",GradientType=1);
                    display: flex;
                    padding: 0 20px;
                  }

                  .brand__text {
                    color: #fff;
                    font-size: 30px;
                    font-style: normal;
                    font-weight: 700;
                    padding-left: 16px;
                  }

                  .footer__disclaimer {
                    color: #9ea4b0;
                    font-size: 12px;
                    padding: 20px 0;
                  }
                </style>
              </head>
              <body>
                <center class="wrapper">
                  <table class="main">
                    <tr>
                      <td>
                        <table>
                          <tr>
                            <td>
                              <div class="brand border--top">
                                <h2 class="brand__text">Jokati</h2>
                              </div>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>

                    <!-- Main -->

                    <tr>
                      <td class="container" style="padding-top: 20px">
                        <table style="min-height: 350px">
                          <tr>
                            <td style="vertical-align: top">
                              <p>Sehr geehrte Damen und Herren,</p>

                              <p>
                                Sie haben ein Angebot von der Firma <b>%s</b> erhalten.
                              </p>

                              <br />

                              <p>
                                Dieses können Sie ab sofort in Ihrem Jokati-Account einsehen.
                              </p>

                              <br />

                              <p>
                                Mit freundlichen Grüßen,<br />
                                Ihr Team Jokati
                              </p>

                              <br />

                              <table>
                                <tbody>
                                  <tr>
                                    <td style="width: 1px;">Telefon:</td>
                                    <td style="padding-left: 5px;">tel:+4940210928510040/210 928 510</a></td>
                                  </tr>
                                  <tr>
                                    <td style="width: 1px;">E-Mail:</td>
                                    <td style="padding-left: 5px;">mailto:info@jokati.appinfo@jokati.app</a></td>
                                  </tr>
                                  <tr>
                                    <td style="width: 1px;">Home:</td>
                                    <td style="padding-left: 5px;">https://www.jokati.dewww.jokati.de</a></td>
                                  </tr>
                                </tbody>
                              </table>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>

                    <tr>
                      <td>
                        <br />
                      </td>
                    </tr>

                    <!-- Footer -->

                    <tr>
                      <td class="container">
                        <table>
                          <tr>
                            <td style="padding: 20px 0">
                              <p class="separator"></p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>

                    <tr>
                      <td class="container">
                        <table class="footer__disclaimer">
                          <tr>
                            <td>
                              <p>Jokati GmbH · Karnapp 25 · D-21079 Hamburg</p>
                              <p>UST.-ID-Nr.: DE3541611057 · Amtsgericht Hamburg HRB – 175722</p>
                              <p>Geschäftsführer: Jan – Peter Richter</p>
                            </td>
                          </tr>

                          <tr>
                            <td>
                              <br />
                            </td>
                          </tr>

                          <tr>
                            <td>
                              <p>
                                Diese Nachricht sowie eventuelle Anlagen sind ausschließlich für die
                                bestimmungsgemäße Verwendung durch den Adressaten gedacht. Die Verwendung durch
                                bzw. die Weiterleitung an Dritte ist daher nicht gestattet.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>

                    <tr>
                      <td>
                        <table>
                          <tr>
                            <td class="border border--bottom"></td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </center>
              </body>
            </html>
            """.formatted(safeCompany);
    }
    
    

public static String ausschreibung(String shipperProjectId, String shipperCompany,
                                       String shipperProjectName, String recipientEmail) {
        String safeProjectId = HtmlUtils.htmlEscape(shipperProjectId == null ? "" : shipperProjectId);
        String safeCompany   = HtmlUtils.htmlEscape(shipperCompany == null ? "" : shipperCompany);
        String safeProject   = HtmlUtils.htmlEscape(shipperProjectName == null ? "" : shipperProjectName);
        String safeRecipient = HtmlUtils.htmlEscape(recipientEmail == null ? "" : recipientEmail);

        return """
            <html>
              <head>
                <meta charset="utf-8"/>
                <title>Jokati Ausschreibung</title>
                <style>
                  body{background:#ffffff;color:#4e5d78;margin:0;font-family:Tahoma,sans-serif}
                  .container{max-width:600px;margin:0 auto;padding:16px}
                  .brand{background:#287fb8;color:#fff;padding:12px 16px;border-radius:6px 6px 0 0}
                  .footer{color:#9ea4b0;font-size:12px;padding:20px 0}
                  .separator{background:#dee7eb;height:1px;margin:20px 0}
                </style>
              </head>
              <body>
                <div class="container">
                  <div class="brand"><h2>Jokati</h2></div>

                  <p>Sehr geehrte Damen und Herren,</p>
                  <p>Sie sind eingeladen, ein Angebot für das Projekt <b>%s</b> (%s) der Firma <b>%s</b> abzugeben.</p>

                  <p>Diese Einladung richtet sich an: <b>%s</b>.</p>

                  <p>Bitte melden Sie sich in Ihrem Jokati-Account an, um die Details einzusehen und Ihr Angebot einzureichen.</p>

                  <br/>
                  <p>Mit freundlichen Grüßen,<br/>Ihr Team Jokati</p>

                  <div class="separator"></div>
                  <div class="footer">
                    <p>Jokati GmbH · Karnapp 25 · D-21079 Hamburg</p>
                    <p>UST.-ID-Nr.: DE3541611057 · Amtsgericht Hamburg HRB – 175722</p>
                    <p>Geschäftsführer: Jan – Peter Richter</p>
                  </div>
                </div>
              </body>
            </html>
            """.formatted(safeProject, safeProjectId, safeCompany, safeRecipient);
    }

}
