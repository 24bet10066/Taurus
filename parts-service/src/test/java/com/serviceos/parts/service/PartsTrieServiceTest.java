package com.serviceos.parts.service;

import com.serviceos.parts.dto.response.PartSearchResult;
import com.serviceos.parts.entity.SparePart;
import com.serviceos.parts.repository.SparePartRepository;
import com.serviceos.parts.service.trie.PartsTrieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PartsTrieServiceTest {

    @Mock SparePartRepository sparePartRepository;

    PartsTrieService trieService;

    private SparePart compressor;
    private SparePart filter;
    private SparePart whirlpoolFan;

    @BeforeEach
    void setUp() {
        compressor = makePart("LG AC Compressor", "AC-COMP-001", "LG", 10);
        filter     = makePart("Samsung Fridge Water Filter", "FWF-002", "Samsung", 5);
        whirlpoolFan = makePart("Whirlpool AC Fan Motor", "WFAN-003", "Whirlpool", 0);

        given(sparePartRepository.findAllActive()).willReturn(List.of(compressor, filter, whirlpoolFan));
        trieService = new PartsTrieService(sparePartRepository);
        trieService.build();
    }

    @Test
    @DisplayName("Exact word match returns the part")
    void exactWordMatch() {
        List<PartSearchResult> results = trieService.search("compressor", 10);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("LG AC Compressor");
    }

    @Test
    @DisplayName("Prefix match returns all parts with that token prefix")
    void prefixMatch_returnsMultiple() {
        // Both "LG AC Compressor" and "Whirlpool AC Fan Motor" have token "ac"
        List<PartSearchResult> results = trieService.search("ac", 10);
        assertThat(results).extracting(PartSearchResult::name)
                .contains("LG AC Compressor", "Whirlpool AC Fan Motor");
    }

    @Test
    @DisplayName("Search by brand returns all parts of that brand")
    void brandSearch() {
        List<PartSearchResult> results = trieService.search("samsung", 10);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).sku()).isEqualTo("FWF-002");
    }

    @Test
    @DisplayName("Search by SKU prefix returns matching part")
    void skuSearch() {
        List<PartSearchResult> results = trieService.search("ac-co", 10);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("LG AC Compressor");
    }

    @Test
    @DisplayName("Single-char prefix returns empty list")
    void tooShortPrefix_returnsEmpty() {
        assertThat(trieService.search("a", 10)).isEmpty();
    }

    @Test
    @DisplayName("Null prefix returns empty list")
    void nullPrefix_returnsEmpty() {
        assertThat(trieService.search(null, 10)).isEmpty();
    }

    @Test
    @DisplayName("Unknown prefix returns empty list")
    void noMatch_returnsEmpty() {
        assertThat(trieService.search("zzz", 10)).isEmpty();
    }

    @Test
    @DisplayName("Results sorted by stock descending")
    void sortedByStockDesc() {
        // compressor=10, filter=5, whirlpoolFan=0
        List<PartSearchResult> results = trieService.search("ac", 10);
        // compressor (10) should come before whirlpoolFan (0)
        assertThat(results.get(0).currentStock())
                .isGreaterThanOrEqualTo(results.get(results.size() - 1).currentStock());
    }

    @Test
    @DisplayName("Limit parameter caps the result size")
    void limitRespected() {
        List<PartSearchResult> results = trieService.search("ac", 1);
        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("insert() makes a new part immediately searchable")
    void insertMakesPartSearchable() {
        SparePart newPart = makePart("Daikin AC PCB Board", "PCB-D-010", "Daikin", 3);
        trieService.insert(newPart);

        List<PartSearchResult> results = trieService.search("daikin", 10);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).sku()).isEqualTo("PCB-D-010");
    }

    @Test
    @DisplayName("rebuild() replaces trie with current DB state")
    void rebuildRefreshesState() {
        // Simulate DB now has only compressor
        given(sparePartRepository.findAllActive()).willReturn(List.of(compressor));
        trieService.rebuild();

        // filter should no longer appear
        assertThat(trieService.search("samsung", 10)).isEmpty();
        // compressor still works
        assertThat(trieService.search("comp", 10)).hasSize(1);
    }

    // -------------------------------------------------------------------------

    private SparePart makePart(String name, String sku, String brand, int stock) {
        SparePart p = new SparePart();
        p.setId(UUID.randomUUID());
        p.setName(name);
        p.setSku(sku);
        p.setBrand(brand);
        p.setCurrentStock(stock);
        p.setSellPrice(BigDecimal.valueOf(500));
        p.setActive(true);
        return p;
    }
}
