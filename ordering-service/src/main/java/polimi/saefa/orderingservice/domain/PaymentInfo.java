package polimi.saefa.orderingservice.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@NoArgsConstructor
public class PaymentInfo {
    private String cardNumber;
    private int expMonth;
    private int expYear;
    private String cvv;

    public PaymentInfo(String cardNumber, int expMonth, int expYear, String cvv) throws PaymentDetailsNotValidException {
        String pattern = "\\d+";
        Pattern r = Pattern.compile(pattern);
        if (!r.matcher(cardNumber).find() || cardNumber.length()!=16)
            throw new PaymentDetailsNotValidException("Card number not valid");
        if (!r.matcher(cardNumber).find() || cvv.length()!=3)
            throw new PaymentDetailsNotValidException("CVV not valid");
        if(expMonth<0 || expMonth>12 || expYear< Calendar.getInstance().get(Calendar.YEAR) ||
                (expYear == Calendar.getInstance().get(Calendar.YEAR) && expMonth<Calendar.getInstance().get(Calendar.MONTH)))
            throw new PaymentDetailsNotValidException("Card expiration not valid");

        this.cardNumber = cardNumber;
        this.expMonth = expMonth;
        this.expYear = expYear;
        this.cvv = cvv;
    }

    public static class PaymentDetailsNotValidException extends Exception{
        public PaymentDetailsNotValidException(String message) {
            super(message);
        }
    }
}
