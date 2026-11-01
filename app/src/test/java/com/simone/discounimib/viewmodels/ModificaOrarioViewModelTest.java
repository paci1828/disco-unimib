package com.simone.discounimib.viewmodels;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;
import androidx.lifecycle.SavedStateHandle;
import com.simone.discounimib.models.Professore;
import com.simone.discounimib.repositories.OrarioDocenteDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ModificaOrarioViewModelTest {
    private static class Fonte implements OrarioDocenteDataSource {
        int caricamenti, salvataggi;
        Caricamento lettura;
        Esito scrittura;
        String salvatoInizio, salvataFine;
        @Override public void caricaOrario(String uid, Caricamento callback) {
            caricamenti++;lettura=callback;
        }
        @Override public void salvaOrario(String uid, String giorno, String inizio, String fine, int durata, Esito callback) {
            salvataggi++;scrittura=callback;salvatoInizio=inizio;salvataFine=fine;
        }
        void letto() {
            lettura.onSuccess(new Professore("p","p@example.com","Nome","Cognome","professore",
                    null,"Lunedì","09:00","12:00",30));
        }
    }
    @Before public void executorSincrono() {
        ArchTaskExecutor.getInstance().setDelegate(new TaskExecutor() {
            @Override public void executeOnDiskIO(Runnable r) { r.run(); }
            @Override public void postToMainThread(Runnable r) { r.run(); }
            @Override public boolean isMainThread() { return true; }
        });
    }
    @After public void resetExecutor() { ArchTaskExecutor.getInstance().setDelegate(null); }

    @Test public void erroreDiPermessiNonRestaInCaricamentoESiPuoRiprovare() {
        Fonte fonte=new Fonte();
        ModificaOrarioViewModel vm=new ModificaOrarioViewModel(new SavedStateHandle(),fonte);
        vm.inizializza("p");
        fonte.lettura.onError("PERMISSION_DENIED");
        assertFalse(vm.occupato());assertTrue(vm.daRicaricare());
        assertEquals("PERMISSION_DENIED",vm.getCaricamento().getValue().errore);
        vm.ricarica();assertEquals(2,fonte.caricamenti);
        fonte.letto();
        assertFalse(vm.daRicaricare());assertFalse(vm.occupato());
        assertEquals("09:00",vm.getInizio());
    }
    @Test public void campiValidiSalvatiUnaVoltaESuccessoSbloccaIlForm() {
        Fonte fonte=new Fonte();
        ModificaOrarioViewModel vm=new ModificaOrarioViewModel(new SavedStateHandle(),fonte);
        vm.inizializza("p");fonte.letto();
        vm.salva();vm.salva();
        assertEquals(1,fonte.salvataggi);assertTrue(vm.occupato());
        fonte.scrittura.onSuccess();
        assertFalse(vm.occupato());
        assertEquals(Boolean.TRUE,vm.getSalvataggio().getValue().dati);
        vm.salva();assertEquals(2,fonte.salvataggi);
    }
    @Test public void fallimentoScritturaMantieneValoriEConsenteNuovoTentativo() {
        Fonte fonte=new Fonte();
        ModificaOrarioViewModel vm=new ModificaOrarioViewModel(new SavedStateHandle(),fonte);
        vm.inizializza("p");fonte.letto();
        vm.setFine("13:00");vm.salva();
        fonte.scrittura.onError("accesso negato");
        assertFalse(vm.occupato());assertEquals("13:00",vm.getFine());
        assertEquals("accesso negato",vm.getSalvataggio().getValue().errore);
        vm.salva();assertEquals(2,fonte.salvataggi);assertEquals("13:00",fonte.salvataFine);
    }
    @Test public void valoriNonValidiNonArrivanoAlRepository() {
        Fonte fonte=new Fonte();
        ModificaOrarioViewModel vm=new ModificaOrarioViewModel(new SavedStateHandle(),fonte);
        vm.inizializza("p");fonte.letto();
        vm.setDurata("0");vm.salva();
        assertEquals(0,fonte.salvataggi);assertNotNull(vm.getSalvataggio().getValue().errore);
        assertFalse(vm.occupato());
    }
    @Test public void bozzaSopravviveAllaRicreazioneENonVieneSovrascrittaDalCaricamento() {
        SavedStateHandle stato=new SavedStateHandle();Fonte fonte=new Fonte();
        ModificaOrarioViewModel vm=new ModificaOrarioViewModel(stato,fonte);
        vm.inizializza("p");fonte.letto();vm.setInizio("09:30");vm.setFine("13:30");
        Fonte altraFonte=new Fonte();
        ModificaOrarioViewModel ricreato=new ModificaOrarioViewModel(stato,altraFonte);
        ricreato.inizializza("p");altraFonte.letto();
        assertEquals("09:30",ricreato.getInizio());assertEquals("13:30",ricreato.getFine());
    }
    @Test public void ricreazioneVistaNonDuplicaCaricamentoInCorso() {
        Fonte fonte=new Fonte();
        ModificaOrarioViewModel vm=new ModificaOrarioViewModel(new SavedStateHandle(),fonte);
        vm.inizializza("p");vm.inizializza("p");
        assertEquals(1,fonte.caricamenti);
    }
    @Test public void salvataggioSenzaProfiloAutorizzatoRiprovaLetturaSenzaScrivere() {
        Fonte fonte=new Fonte();
        ModificaOrarioViewModel vm=new ModificaOrarioViewModel(new SavedStateHandle(),fonte);
        vm.inizializza("p");fonte.lettura.onError("permessi");
        vm.setGiorno("Lunedì");vm.setInizio("09:00");vm.setFine("12:00");vm.setDurata("30");
        vm.salva();
        assertEquals(0,fonte.salvataggi);assertEquals(2,fonte.caricamenti);
    }
    @Test public void rispostaDelProfiloPrecedenteNonSovrascriveQuelloNuovo() {
        Fonte fonte=new Fonte();
        ModificaOrarioViewModel vm=new ModificaOrarioViewModel(new SavedStateHandle(),fonte);
        vm.inizializza("p");
        OrarioDocenteDataSource.Caricamento vecchio=fonte.lettura;
        vm.inizializza("altro");
        vecchio.onSuccess(new Professore("p","p@example.com","Vecchio","Profilo","professore",
                null,"Lunedì","09:00","12:00",30));
        assertTrue(vm.occupato());assertEquals("",vm.getGiorno());
        fonte.lettura.onError("profilo non autorizzato");
        assertTrue(vm.daRicaricare());
    }
}
