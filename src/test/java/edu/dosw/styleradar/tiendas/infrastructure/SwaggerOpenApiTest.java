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
                .andExpect(jsonPath("$.paths['/api/v1/almacenes'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/almacenes'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/almacenes/{nit}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/almacenes/{nit}/configuracion-catalogo'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/almacenes/{nit}/configuracion-catalogo'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/almacenes/{nit}/umbral-ultimas-unidades'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/almacenes/{nit}/umbral-ultimas-unidades'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/almacenes/{nit}/dias-nueva-llegada'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/almacenes/{nit}/dias-nueva-llegada'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/almacenes/{nit}/prendas/{itemId}/ultimas-unidades'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/v1/almacenes/{nit}/prendas/{itemId}/nueva-llegada'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/v1/almacenes/{nit}/fotografias'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/almacenes/{nit}/fotografias'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/almacenes/{nit}/fotografias/{fotoId}'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/almacenes/verificacion'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/almacenes/{nit}/verificacion'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/almacenes/{nit}/verificacion'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/almacenes/{nit}/verificacion/solicitar-revision'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/almacenes/{nit}/administradores/{usuarioId}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/almacenes/{nit}/administradores'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/fundaciones'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/fundaciones'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/fundaciones/{id}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/fundaciones/{id}/desactivar'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/v1/fundaciones'].get").exists());
    }

    @Test
    void debeExponerInterfazSwaggerUi() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
