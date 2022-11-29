package it.polimi.sefa.restclient.externalrestapi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartInstanceResponse {
    private String serviceImplementationName;
    private String address;
    private int port;
}
