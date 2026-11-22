package edu.dosw.styleradar.tiendas.infrastructure;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.repository.SpringDataAdministradorAlmacenRepository;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.repository.SpringDataAlmacenRepository;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.repository.SpringDataFotografiaTiendaRepository;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.repository.SpringDataFundacionRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SwaggerOpenApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SpringDataAlmacenRepository springDataAlmacenRepository;

    @MockBean
    private SpringDataAdministradorAlmacenRepository springDataAdministradorAlmacenRepository;

    @MockBean
    private SpringDataFundacionRepository springDataFundacionRepository;

    @MockBean
    private SpringDataFotografiaTiendaRepository springDataFotografiaTiendaRepository;

    @Test
    void debeExponerEspecificacionOpenApiYEndpointsDeTiendas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("StyleRadar - Stores Service API (ms-tiendas)"))
                .andExpect(jsonPath("$.paths['/api/v1/stores'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/stores'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/stores/{nit}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/stores/{nit}/catalog-settings'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/stores/{nit}/catalog-settings'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/stores/{nit}/low-stock-threshold'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/stores/{nit}/low-stock-threshold'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/stores/{nit}/new-arrival-days'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/stores/{nit}/new-arrival-days'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/stores/{nit}/items/{itemId}/low-stock'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/v1/stores/{nit}/items/{itemId}/new-arrival'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/v1/stores/{nit}/photos'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/stores/{nit}/photos'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/stores/{nit}/photos/{photoId}'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/stores/verifications'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/stores/{nit}/verification'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/stores/{nit}/verification'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/stores/{nit}/verification/review-request'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/stores/{nit}/administrators/{userId}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/stores/{nit}/administrators'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/foundations'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/foundations'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/foundations/{id}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/foundations/{id}/deactivate'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/v1/foundations'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/stores/{nit}'].get.security").isEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/foundations'].get.security").isEmpty());
    }

    @Test
    void debeExponerInterfazSwaggerUi() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
