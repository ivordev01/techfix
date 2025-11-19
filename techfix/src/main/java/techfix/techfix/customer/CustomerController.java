package techfix.techfix.customer;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

	private final CustomerService customerService;

	public CustomerController(CustomerService customerService) {
		this.customerService = customerService;
	}

	@GetMapping
	public List<CustomerResponse> getAllCustomers() {
		return customerService.findAll().stream().map(CustomerResponse::from).toList();
	}

	@GetMapping("/search")
	public List<CustomerResponse> searchCustomers(
			@RequestParam @Size(min = 2, message = "Informe ao menos 2 caracteres") String query) {
		return customerService.search(query).stream().map(CustomerResponse::from).toList();
	}

	@PostMapping
	public CustomerResponse createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
		var created = customerService.create(request.name(), request.cpf(), request.phone(), request.address());
		return CustomerResponse.from(created);
	}

	public record CustomerResponse(String id, String name, String cpf, String phone, String address) {
		public static CustomerResponse from(CustomerService.Customer customer) {
			return new CustomerResponse(customer.id(), customer.name(), customer.cpf(), customer.phone(),
					customer.address());
		}
	}

	public record CreateCustomerRequest(
			@NotBlank(message = "Nome é obrigatório") @Size(max = 120) String name,
			@NotBlank(message = "CPF é obrigatório") @Pattern(regexp = "\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}", message = "CPF deve estar no formato 000.000.000-00") String cpf,
			@NotBlank(message = "Telefone é obrigatório") @Size(max = 20) String phone,
			@NotBlank(message = "Endereço é obrigatório") @Size(max = 255) String address) {
	}
}

