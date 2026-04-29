package eu.bmdproject.dataspace.controller;

import eu.bmdproject.dataspace.dao.ROCrateDao;
import eu.bmdproject.dataspace.exception.BadRequestException;
import eu.bmdproject.dataspace.component.ROCrateUriValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static eu.bmdproject.dataspace.model.RDFFormat.TURTLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ROCrateControllerTest {

  private ROCrateDao dao;
  private ROCrateUriValidator uriValidator;
  private ROCrateController controller;

  @BeforeEach
  void setUp() {
    dao = mock(ROCrateDao.class);
    uriValidator = mock(ROCrateUriValidator.class);
    controller = new ROCrateController(dao, uriValidator);
  }

  @Test
  void ingest_validatesUriThenDelegatesToDao() {
    //Given
    URI uri = URI.create("https://example.com/crate.zip");
    ROCrateDao.IngestResult result = new ROCrateDao.IngestResult("abc123", "urn:graph:abc123");
    when(dao.ingestFromUrl(uri)).thenReturn(result);

    //When
    ResponseEntity<ROCrateDao.IngestResult> response = controller.ingest(uri);

    //Then
    assertThat(response.getStatusCode().value()).isEqualTo(201);
    assertThat(response.getHeaders().getLocation()).isEqualTo(URI.create("/rocrates/abc123"));
    assertThat(response.getBody()).isEqualTo(result);
    verify(uriValidator).validate(uri);
    verify(dao).ingestFromUrl(uri);
  }

  @Test
  void ingest_validatorRejects_daoIsNeverCalled() {
    //Given
    URI uri = URI.create("http://example.com/crate.zip");
    doThrow(new BadRequestException("Only https URIs are allowed")).when(uriValidator).validate(uri);

    //When/Then
    assertThatThrownBy(() -> controller.ingest(uri))
        .isInstanceOf(BadRequestException.class);
    verifyNoInteractions(dao);
  }

  @Test
  void list_returnsOkWithDaoResult() {
    //Given
    List<ROCrateDao.CrateRef> refs = List.of(new ROCrateDao.CrateRef("abc123", "urn:graph:abc123"));
    when(dao.list()).thenReturn(refs);

    //When
    ResponseEntity<List<ROCrateDao.CrateRef>> response = controller.list();

    //Then
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isEqualTo(refs);
  }

  @Test
  void getGraph_found_returnsOkWithCorrectContentType() {
    //Given
    ROCrateDao.GraphPayload payload = new ROCrateDao.GraphPayload("urn:graph:abc123", "<rdf/>", "text/turtle");
    when(dao.getMetadata("abc123", TURTLE)).thenReturn(Optional.of(payload));

    //When
    ResponseEntity<String> response = controller.getGraph("abc123", TURTLE);

    //Then
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getHeaders().getContentType()).hasToString("text/turtle");
    assertThat(response.getBody()).isEqualTo("<rdf/>");
  }

  @Test
  void getGraph_notFound_returns404() {
    //Given
    when(dao.getMetadata("missing", TURTLE)).thenReturn(Optional.empty());

    //When
    ResponseEntity<String> response = controller.getGraph("missing", TURTLE);

    //Then
    assertThat(response.getStatusCode().value()).isEqualTo(404);
  }

  @Test
  void getSummary_found_returnsOk() {
    //Given
    ROCrateDao.CrateSummary summary = new ROCrateDao.CrateSummary("abc123", "urn:graph:abc123", 42L);
    when(dao.getSummary("abc123")).thenReturn(Optional.of(summary));

    //When
    ResponseEntity<ROCrateDao.CrateSummary> response = controller.getSummary("abc123");

    //Then
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isEqualTo(summary);
  }

  @Test
  void getSummary_notFound_returns404() {
    //Given
    when(dao.getSummary("missing")).thenReturn(Optional.empty());

    //When/Then
    assertThat(controller.getSummary("missing").getStatusCode().value()).isEqualTo(404);
  }

  @Test
  void delete_existing_returnsNoContent() {
    //Given
    when(dao.delete("abc123")).thenReturn(true);

    //When/Then
    assertThat(controller.delete("abc123").getStatusCode().value()).isEqualTo(204);
  }

  @Test
  void delete_notFound_returns404() {
    //Given
    when(dao.delete("missing")).thenReturn(false);

    //When/Then
    assertThat(controller.delete("missing").getStatusCode().value()).isEqualTo(404);
  }
}