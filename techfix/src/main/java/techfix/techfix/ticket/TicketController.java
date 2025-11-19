package techfix.techfix.ticket;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import techfix.techfix.customer.CustomerService;
import techfix.techfix.report.PdfReportService;
import techfix.techfix.ticket.TicketService.Ticket;
import techfix.techfix.ticket.TicketService.TicketStatus;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

	private final TicketService ticketService;
	private final CustomerService customerService;
	private final PdfReportService pdfReportService;

	public TicketController(TicketService ticketService, CustomerService customerService,
			PdfReportService pdfReportService) {
		this.ticketService = ticketService;
		this.customerService = customerService;
		this.pdfReportService = pdfReportService;
	}

	@GetMapping
	public List<TicketResponse> getAllTicketsEndpoint() {
		return ticketService.findAll().stream().map(TicketResponse::from).toList();
	}

	@PostMapping
	public TicketResponse createTicket(@Valid @RequestBody CreateTicketRequest request) {
		var ticket = ticketService.create(request.customerId(), request.deviceType(), request.deviceModel(),
				request.description());
		return TicketResponse.from(ticket);
	}

	@PostMapping("/{ticketId}/budget")
	public ResponseEntity<byte[]> generateBudget(@PathVariable String ticketId,
			@Valid @RequestBody BudgetRequest request) {
		var ticket = ticketService.getRequired(ticketId);

		byte[] pdf = pdfReportService.generateBudgetPdf(ticket, customerService.getRequired(ticket.customerId()), request);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orcamento-" + ticketId + ".pdf")
				.contentType(MediaType.APPLICATION_PDF).body(pdf);
	}

	@PostMapping("/{ticketId}/laudo")
	public ResponseEntity<byte[]> generateLaudo(@PathVariable String ticketId, @Valid @RequestBody LaudoRequest request) {
		var ticket = ticketService.getRequired(ticketId);

		byte[] pdf = pdfReportService.generateLaudoPdf(ticket, customerService.getRequired(ticket.customerId()), request);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=laudo-" + ticketId + ".pdf")
				.contentType(MediaType.APPLICATION_PDF).body(pdf);
	}

	public record TicketResponse(String id, String customerId, String customerName, String device, String description,
			TicketStatus status, String entryDate) {
		public static TicketResponse from(Ticket ticket) {
			return new TicketResponse(ticket.id(), ticket.customerId(), ticket.customerName(), ticket.device(),
					ticket.description(), ticket.status(), ticket.entryDate());
		}
	}

	public record CreateTicketRequest(
			@NotBlank(message = "Cliente é obrigatório") String customerId,
			@NotBlank(message = "Tipo do equipamento é obrigatório") @Size(max = 60) String deviceType,
			@NotBlank(message = "Modelo é obrigatório") @Size(max = 60) String deviceModel,
			@NotBlank(message = "Descrição é obrigatória") @Size(min = 5, max = 400) String description) {
	}

	public record BudgetRequest(
			@NotBlank(message = "Diagnóstico é obrigatório") String diagnosis,
			@NotBlank(message = "Causa provável é obrigatória") String cause,
			@NotNull Boolean equipmentHasConsent,
			@NotNull Boolean powerSupplyAffected,
			@Valid BudgetItem part,
			@Valid LaborInfo laborInfo) {

		public record BudgetItem(
				@Size(max = 120) String name,
				@Size(max = 200) String specification,
				@Size(max = 40) String code,
				@DecimalMin(value = "0.0", inclusive = true, message = "Preço da peça não pode ser negativo") BigDecimal partPrice,
				@DecimalMin(value = "0.0", inclusive = true, message = "Mão-de-obra não pode ser negativa") BigDecimal laborPrice) {
		}

		public record LaborInfo(
				@Size(max = 120) String description,
				@Size(max = 60) String estimatedTime,
				@Size(max = 120) String responsible) {
		}
	}

	public record LaudoRequest(
			@NotBlank(message = "Condição do equipamento é obrigatória") String equipmentCondition,
			@NotBlank(message = "Queixa do cliente é obrigatória") String problemDescription,
			@NotBlank(message = "Diagnóstico é obrigatório") String diagnostic,
			@NotBlank(message = "Ações executadas são obrigatórias") String actionsTaken,
			@NotBlank(message = "Recomendações são obrigatórias") String recommendations,
			@NotBlank(message = "Responsável técnico é obrigatório") String technicianName) {
	}
}
