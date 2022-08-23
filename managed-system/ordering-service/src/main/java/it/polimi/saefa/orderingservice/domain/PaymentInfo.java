package it.polimi.saefa.orderingservice.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import it.polimi.saefa.orderingservice.exceptions.PaymentDetailsNotValidException;

import java.util.Calendar;
import java.util.regex.Pattern;

@Data
@NoArgsConstructor
public class PaymentInfo {
    private String cardNumber;
    private int expMonth;
    private int expYear;
    private String cvv;

    public PaymentInfo(String cardNumber, int expMonth, int expYear, String cvv){
        String pattern = "\\d+";
        Pattern r = Pattern.compile(pattern);
        if (!r.matcher(cardNumber).find() || cardNumber.length()!=16)
            throw new PaymentDetailsNotValidException("Card number not valid");
        if (!r.matcher(cardNumber).find() || cvv.length()!=3)
            throw new PaymentDetailsNotValidException("CVV not valid");
        if (expMonth<0 || expMonth>12 || expYear < Calendar.getInstance().get(Calendar.YEAR)%100 ||
                (expYear == Calendar.getInstance().get(Calendar.YEAR)%100 && expMonth<Calendar.getInstance().get(Calendar.MONTH)+1))
            throw new PaymentDetailsNotValidException("Card expiration not valid. Provided:" + expMonth + "/" + expYear +
                    " while current is " + (Calendar.getInstance().get(Calendar.MONTH)+1) + "/" + Calendar.getInstance().get(Calendar.YEAR));

        this.cardNumber = cardNumber;
        this.expMonth = expMonth;
        this.expYear = expYear;
        this.cvv = cvv;
    }

}
