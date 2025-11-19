package techfix.techfix.inventory;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import techfix.techfix.report.PdfReportService;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

	private final InventoryService inventoryService;
	private final PdfReportService pdfReportService;

	public InventoryController(InventoryService inventoryService, PdfReportService pdfReportService) {
		this.inventoryService = inventoryService;
		this.pdfReportService = pdfReportService;
	}

	@GetMapping
	public List<InventoryService.InventoryItem> getAllItems() {
		return inventoryService.findAll();
	}

	@PostMapping
	public InventoryService.InventoryItem createItem(@Valid @RequestBody CreateInventoryItemRequest request) {
		return inventoryService.create(request.type(), request.brand(), request.quantity(), request.price());
	}

	@GetMapping("/report")
	public ResponseEntity<byte[]> generateInventoryReport() {
		byte[] pdf = pdfReportService.generateInventoryReport(inventoryService.snapshot());
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=estoque-techfix.pdf")
				.contentType(MediaType.APPLICATION_PDF).body(pdf);
	}

	public record CreateInventoryItemRequest(
			@NotBlank(message = "Tipo é obrigatório") @Size(max = 80) String type,
			@NotBlank(message = "Marca é obrigatória") @Size(max = 60) String brand,
			@Min(value = 1, message = "Quantidade mínima é 1") int quantity,
			@NotNull(message = "Preço é obrigatório") @DecimalMin(value = "0.0", inclusive = true, message = "Preço não pode ser negativo") BigDecimal price) {
	}
}

