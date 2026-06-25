package dz.gam.poste.indicateursmock;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * API d'ADMINISTRATION des chiffres mock (substitut du Cube Power BI). Permet de lire et de
 * MODIFIER les KPI d'agence et la situation mensuelle, par agence (feuille). Demain, ces
 * valeurs proviendront du Cube Power BI : ce contrôleur et le store disparaîtront au profit
 * d'un adapter Power BI, sans toucher au domaine ni aux écrans.
 */
@RestController
@RequestMapping("/api/mock/indicateurs")
public class IndicateursMockController {

    private final MesuresAgenceJpaRepository mesures;
    private final SituationMensuelleJpaRepository situations;

    public IndicateursMockController(MesuresAgenceJpaRepository mesures,
                                     SituationMensuelleJpaRepository situations) {
        this.mesures = mesures;
        this.situations = situations;
    }

    // ----- KPI d'agence -----

    @GetMapping
    public List<MesuresAgenceDto> listerMesures() {
        return mesures.findAll().stream().map(MesuresAgenceDto::de).toList();
    }

    @GetMapping("/{code}")
    public MesuresAgenceDto mesures(@PathVariable String code) {
        return MesuresAgenceDto.de(mesures.findByCodeAgence(code)
                .orElseThrow(() -> new NoSuchElementException("Agence inconnue : " + code)));
    }

    @PutMapping("/{code}")
    public MesuresAgenceDto majMesures(@PathVariable String code, @RequestBody MesuresAgenceDto dto) {
        MesuresAgenceEntity e = mesures.findByCodeAgence(code).orElseGet(MesuresAgenceEntity::new);
        e.codeAgence = code;
        e.caYtdN = dto.caYtdN();
        e.caYtdN1 = dto.caYtdN1();
        e.caMoisN = dto.caMoisN();
        e.caMoisM1 = dto.caMoisM1();
        e.productionMois = dto.productionMois();
        e.encaisseMois = dto.encaisseMois();
        e.deposeMois = dto.deposeMois();
        e.encaisseCumul = dto.encaisseCumul();
        e.deposeCumul = dto.deposeCumul();
        e.echuNonEncaisse = dto.echuNonEncaisse();
        e.echuNonEncaisseM1 = dto.echuNonEncaisseM1();
        e.encaissementsLettres = dto.encaissementsLettres();
        e.encaissementsLettresM1 = dto.encaissementsLettresM1();
        e.sinistres12m = dto.sinistres12m();
        e.primes12m = dto.primes12m();
        e.sinistres12mN1 = dto.sinistres12mN1();
        e.primes12mN1 = dto.primes12mN1();
        e.contratsActifs = dto.contratsActifs();
        e.contratsActifsVariation = dto.contratsActifsVariation();
        return MesuresAgenceDto.de(mesures.save(e));
    }

    // ----- Situation mensuelle -----

    @GetMapping("/{code}/situation")
    public List<SituationMensuelleDto> listerSituations(@PathVariable String code) {
        return situations.findByCodeAgenceOrderByMoisDesc(code).stream().map(SituationMensuelleDto::de).toList();
    }

    @GetMapping("/{code}/situation/{mois}")
    public SituationMensuelleDto situation(@PathVariable String code, @PathVariable String mois) {
        return SituationMensuelleDto.de(situations.findByCodeAgenceAndMois(code, mois)
                .orElseThrow(() -> new NoSuchElementException("Situation inconnue : " + code + " / " + mois)));
    }

    @PutMapping("/{code}/situation/{mois}")
    public SituationMensuelleDto majSituation(@PathVariable String code, @PathVariable String mois,
                                              @RequestBody SituationMensuelleDto dto) {
        SituationMensuelleEntity s = situations.findByCodeAgenceAndMois(code, mois)
                .orElseGet(SituationMensuelleEntity::new);
        s.codeAgence = code;
        s.mois = mois;
        s.productionEmise = dto.productionEmise();
        s.encaisse = dto.encaisse();
        s.verse = dto.verse();
        return SituationMensuelleDto.de(situations.save(s));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail introuvable(NoSuchElementException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
