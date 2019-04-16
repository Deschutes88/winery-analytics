import java.io.StringWriter

import org.apache.commons.csv.{CSVFormat, CSVPrinter, QuoteMode}

import scala.util.Try
val sw = new StringWriter()
val csvPrinter = new CSVPrinter(sw,
  CSVFormat.DEFAULT
      .withQuoteMode(QuoteMode.MINIMAL)
    .withDelimiter("\t".charAt(0))
    .withEscape("\\".charAt(0))
)

csvPrinter.printRecord("this is a test", "and \t12")
csvPrinter.flush()
sw.toString.replace("\t" , " | ")