# saefa
Self Adaptive eFood App


Per eseguire l'applicazione: 

1. lato **server** 
   1. posizionarsi nella cartella principale dell'applicazione
   2. eseguire il comando `gradle restaurant-server:bootRun`

2. lato **client**
   1. posizionarsi nella cartella principale dell'applicazione
   2. eseguire il comando `gradle restaurant-client-rest:bootRun` (utilizza il client sincrono) 
   3. per utilizzare il client asincrono, eseguire il comando `gradle restaurant-async-client-rest:bootRun` 