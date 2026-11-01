package com.simone.discounimib.domain;

import org.junit.Test;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class SincronizzaRicevimentoTest {
    private static class Archivio implements SincronizzaRicevimento.Archivio {
        boolean attuale=true, calendario, completo, fallisciCheckpointEmail, fallisciCalendario, bloccaInvio;
        String email="da_inviare";
        final List<String> ordine=new ArrayList<>();
        @Override public boolean attuale() { return attuale; }
        @Override public void calendarioCompletato(String id) throws Exception {
            if(fallisciCalendario) throw new IOException("checkpoint");
            calendario=true;ordine.add("calendario_salvato");
        }
        @Override public boolean prenotaEmail() {
            if(bloccaInvio) return false;
            email="in_invio";ordine.add("email_prenotata");return true;
        }
        @Override public void emailCompletata() throws Exception {
            if(fallisciCheckpointEmail) throw new IOException("risposta persa");
            email="inviata";
        }
        @Override public void emailDaVerificare() { email="da_verificare"; }
        @Override public void emailRifiutata() { email="da_inviare"; }
        @Override public void completa() { completo=true; }
    }
    private static class Servizi implements SincronizzaRicevimento.Servizi {
        int calendari,inviate,tentativi;
        Exception erroreEmail;
        final Archivio archivio;
        Servizi(Archivio archivio){this.archivio=archivio;}
        @Override public String calendario() {
            calendari++;return ProtocolloSincronizzazione.idEvento("app");
        }
        @Override public void email() throws Exception {
            tentativi++;archivio.ordine.add("email_remota");
            if(erroreEmail!=null)throw erroreEmail;
            inviate++;
        }
    }
    private SincronizzaRicevimento.Esito esegui(Archivio a,Servizi s) throws Exception {
        return new SincronizzaRicevimento().esegui(a.calendario,
                ProtocolloSincronizzazione.email(1,1,a.email,true),a,s);
    }

    @Test public void checkpointPrecedeInvioEIlSecondoTentativoNonDuplica() throws Exception {
        Archivio a=new Archivio();Servizi s=new Servizi(a);
        assertEquals(SincronizzaRicevimento.Esito.COMPLETATO,esegui(a,s));
        assertTrue(a.ordine.indexOf("email_prenotata")<a.ordine.indexOf("email_remota"));
        esegui(a,s);
        assertEquals(1,s.calendari);assertEquals(1,s.inviate);assertTrue(a.completo);
    }
    @Test public void rispostaEmailIncertaBloccaReinvioAutomatico() throws Exception {
        Archivio a=new Archivio();Servizi s=new Servizi(a);
        s.erroreEmail=new IOException("risposta persa dopo possibile consegna");
        assertEquals(SincronizzaRicevimento.Esito.EMAIL_DA_VERIFICARE,esegui(a,s));
        s.erroreEmail=null;
        assertEquals(SincronizzaRicevimento.Esito.EMAIL_DA_VERIFICARE,esegui(a,s));
        assertEquals(1,s.tentativi);assertFalse(a.completo);
    }
    @Test public void crashDopoInvioPrimaDelCheckpointNonDuplicaAlRiavvio() throws Exception {
        Archivio a=new Archivio();Servizi s=new Servizi(a);a.fallisciCheckpointEmail=true;
        try{esegui(a,s);fail();}catch(IOException attesa){}
        assertEquals("in_invio",a.email);
        a.fallisciCheckpointEmail=false;
        assertEquals(SincronizzaRicevimento.Esito.EMAIL_DA_VERIFICARE,esegui(a,s));
        assertEquals(1,s.inviate);
    }
    @Test public void calendarioNonSalvatoVieneRiconciliatoPrimaDellaEmail() throws Exception {
        Archivio a=new Archivio();Servizi s=new Servizi(a);a.fallisciCalendario=true;
        try{esegui(a,s);fail();}catch(IOException attesa){}
        assertEquals(0,s.inviate);
        a.fallisciCalendario=false;
        esegui(a,s);
        assertEquals(2,s.calendari);assertEquals(1,s.inviate);
    }
    @Test public void revisioneSuperataNonProduceEffetti() throws Exception {
        Archivio a=new Archivio();Servizi s=new Servizi(a);a.attuale=false;
        assertEquals(SincronizzaRicevimento.Esito.SUPERATO,esegui(a,s));
        assertEquals(0,s.calendari);assertEquals(0,s.inviate);
    }
    @Test public void modificaDopoCalendarioFermaInvioEmail() throws Exception {
        Archivio a=new Archivio(){
            @Override public void calendarioCompletato(String id){calendario=true;attuale=false;}
        };
        Servizi s=new Servizi(a);
        assertEquals(SincronizzaRicevimento.Esito.SUPERATO,esegui(a,s));
        assertEquals(0,s.inviate);
    }
    @Test public void mancataPrenotazioneEmailNonInvia() throws Exception {
        Archivio a=new Archivio();Servizi s=new Servizi(a);a.bloccaInvio=true;
        assertEquals(SincronizzaRicevimento.Esito.SUPERATO,esegui(a,s));
        assertEquals(0,s.inviate);
    }
    @Test public void rifiutoEsplicitoConsenteUnNuovoTentativo() throws Exception {
        Archivio a=new Archivio();Servizi s=new Servizi(a);
        s.erroreEmail=new SincronizzaRicevimento.InvioRifiutato(new IOException("HTTP 429"));
        try{esegui(a,s);fail();}catch(SincronizzaRicevimento.InvioRifiutato attesa){}
        assertEquals("da_inviare",a.email);
        s.erroreEmail=null;esegui(a,s);
        assertEquals(1,s.inviate);assertEquals(2,s.tentativi);
    }
    @Test public void cancellazionePerRiprogrammazionePuoSaltareEmail() throws Exception {
        Archivio a=new Archivio();Servizi s=new Servizi(a);
        new SincronizzaRicevimento().esegui(false,
                ProtocolloSincronizzazione.email(1,0,null,false),a,s);
        assertEquals(1,s.calendari);assertEquals(0,s.inviate);assertTrue(a.completo);
    }
    @Test public void emailPrecedenteIncertaBloccaAncheRevisioniSuccessive() {
        assertEquals(ProtocolloSincronizzazione.Email.DA_VERIFICARE,
                ProtocolloSincronizzazione.email(2,1,"in_invio",true));
        assertEquals(ProtocolloSincronizzazione.Email.INVIA,
                ProtocolloSincronizzazione.email(2,1,"inviata",true));
    }
    @Test public void emailGiaGestitaManualeNonVieneInviata() {
        assertEquals(ProtocolloSincronizzazione.Email.SALTA,
                ProtocolloSincronizzazione.email(2,2,"gestita",true));
    }
    @Test public void nuovoEventoAttendeCancellazioneCalendarPrecedente() {
        assertFalse(ProtocolloSincronizzazione.precedenteRiconciliato("annullato",2,1));
        assertTrue(ProtocolloSincronizzazione.precedenteRiconciliato("annullato",2,2));
    }
    @Test public void precedenteAncoraAttivoONonVersionatoNonVaIgnorato() {
        assertFalse(ProtocolloSincronizzazione.precedenteRiconciliato("accettato",2,2));
        assertFalse(ProtocolloSincronizzazione.precedenteRiconciliato("annullato",0,0));
    }
}
