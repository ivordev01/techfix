// Utilitários
const $ = (selector) => {
  const element = document.querySelector(selector);
  if (!element) {
    throw new Error(`Elemento ${selector} não encontrado`);
  }
  return element;
};

const $$ = (selector) => document.querySelectorAll(selector);

// Navegação
const navLinks = $$(".nav-link");
const pages = $$(".page");

navLinks.forEach((link) => {
  link.addEventListener("click", () => {
    const targetPage = link.dataset.page;
    
    // Atualizar links ativos
    navLinks.forEach((l) => l.classList.remove("active"));
    link.classList.add("active");
    
    // Mostrar página correta
    pages.forEach((p) => p.classList.remove("active"));
    $(`#page-${targetPage}`).classList.add("active");
    
    // Carregar dados da página
    if (targetPage === "home") {
      loadDashboard();
    } else if (targetPage === "tickets") {
      loadTickets();
    } else if (targetPage === "customers") {
      loadCustomers();
    } else if (targetPage === "inventory") {
      loadInventory();
    } else if (targetPage === "laudo") {
      loadLaudoTickets();
    }
  });
});

// Dashboard (Menu Principal)
const pipeline = $("#ticket-pipeline");
const pill = $("#pipeline-pill");

const statusLabel = {
  TRIAGEM: "Em triagem",
  EM_ANDAMENTO: "Em andamento",
  FINALIZADO: "Concluído",
};

const setMetricText = (selector, value) => {
  $(selector).textContent = value;
};

const renderTickets = (tickets) => {
  pipeline.innerHTML = "";

  if (!tickets.length) {
    pipeline.innerHTML = '<p class="placeholder">Nenhum chamado no momento.</p>';
    return;
  }

  tickets.forEach((ticket) => {
    const card = document.createElement("article");
    card.className = "ticket-card";
    card.innerHTML = `
      <div>
        <strong>${ticket.customer}</strong>
        <span>${ticket.device}</span>
      </div>
      <div>
        <span>${statusLabel[ticket.status]}</span>
        <strong>${ticket.entryDate}</strong>
      </div>
    `;
    pipeline.appendChild(card);
  });
};

const loadDashboard = async () => {
  pill.textContent = "sincronizando…";
  pill.classList.remove("pill--error");

  try {
    const response = await fetch("/api/dashboard");
    if (!response.ok) {
      throw new Error("Falha ao carregar o painel");
    }

    const summary = await response.json();
    setMetricText("#metric-today", summary.todayTickets.toString());
    setMetricText("#metric-techs", summary.techniciansAvailable.toString());
    pill.textContent = `${summary.pendingTickets} em aberto`;

    renderTickets(summary.tickets);
  } catch (error) {
    console.error(error);
    pill.textContent = "erro";
    pill.classList.add("pill--error");
    pipeline.innerHTML = '<p class="placeholder">Não foi possível sincronizar. Tente novamente.</p>';
  }
};

// Gerenciamento de Clientes
let selectedCustomer = null;
const customerSearch = $("#customer-search");
const customerResults = $("#customer-results");
const selectedCustomerDiv = $("#selected-customer");
const selectedCustomerInfo = $("#selected-customer-info");
const clearCustomerBtn = $("#clear-customer");

let searchTimeout;
customerSearch.addEventListener("input", async (e) => {
  const query = e.target.value.trim();
  
  clearTimeout(searchTimeout);
  
  if (query.length < 2) {
    customerResults.classList.remove("show");
    return;
  }
  
  searchTimeout = setTimeout(async () => {
    try {
      const response = await fetch(`/api/customers/search?query=${encodeURIComponent(query)}`);
      if (!response.ok) return;
      
      const customers = await response.json();
      renderCustomerResults(customers);
    } catch (error) {
      console.error(error);
    }
  }, 300);
});

const renderCustomerResults = (customers) => {
  customerResults.innerHTML = "";
  
  if (customers.length === 0) {
    customerResults.innerHTML = '<div class="customer-result-item">Nenhum cliente encontrado</div>';
    customerResults.classList.add("show");
    return;
  }
  
  customers.forEach((customer) => {
    const item = document.createElement("div");
    item.className = "customer-result-item";
    item.innerHTML = `
      <strong>${customer.name}</strong>
      <small>${customer.id} · ${customer.phone}</small>
    `;
    item.addEventListener("click", () => {
      selectCustomer(customer);
      customerResults.classList.remove("show");
      customerSearch.value = "";
    });
    customerResults.appendChild(item);
  });
  
  customerResults.classList.add("show");
};

const selectCustomer = (customer) => {
  selectedCustomer = customer;
  selectedCustomerInfo.textContent = `${customer.name} (${customer.id})`;
  selectedCustomerDiv.style.display = "flex";
  customerSearch.disabled = true;
};

clearCustomerBtn.addEventListener("click", () => {
  selectedCustomer = null;
  selectedCustomerDiv.style.display = "none";
  customerSearch.disabled = false;
  customerSearch.focus();
});

// Fechar resultados ao clicar fora
document.addEventListener("click", (e) => {
  if (!customerSearch.contains(e.target) && !customerResults.contains(e.target)) {
    customerResults.classList.remove("show");
  }
});

// Formulário de Cliente
const customerForm = $("#customer-form");
customerForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  const formData = new FormData(customerForm);
  
  const customer = {
    name: formData.get("name"),
    cpf: formData.get("cpf"),
    phone: formData.get("phone"),
    address: formData.get("address"),
  };
  
  try {
    const response = await fetch("/api/customers", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(customer),
    });
    
    if (!response.ok) {
      throw new Error(await extractError(response));
    }
    
    const createdCustomer = await response.json();
    showToast(`Cliente cadastrado com sucesso! ID: ${createdCustomer.id}`);
    customerForm.reset();
    loadCustomers();
  } catch (error) {
    showToast("Erro ao cadastrar cliente: " + error.message);
  }
});

const loadCustomers = async () => {
  const customersList = $("#customers-list");
  customersList.innerHTML = '<p class="placeholder">Carregando...</p>';
  
  try {
    const response = await fetch("/api/customers");
    if (!response.ok) throw new Error(await extractError(response));
    
    const customers = await response.json();
    
    if (customers.length === 0) {
      customersList.innerHTML = '<p class="placeholder">Nenhum cliente cadastrado ainda.</p>';
      return;
    }
    
    customersList.innerHTML = "";
    customers.forEach((customer) => {
      const card = document.createElement("div");
      card.className = "customer-card";
      card.innerHTML = `
        <div class="customer-card__header">
          <div>
            <div class="customer-card__id">${customer.id}</div>
            <strong>${customer.name}</strong>
          </div>
          <button class="btn btn--secondary customer-ticket-btn" type="button" data-customer-id="${customer.id}" data-customer-name="${customer.name}">
            Cadastrar Chamado
          </button>
        </div>
        <div class="customer-card__info">
          <div>CPF: ${customer.cpf}</div>
          <div>Telefone: ${customer.phone}</div>
          <div>${customer.address}</div>
        </div>
      `;
      
      // Adicionar event listener ao botão
      const ticketBtn = card.querySelector(".customer-ticket-btn");
      ticketBtn.addEventListener("click", () => {
        navigateToTicketsWithCustomer(customer);
      });
      
      customersList.appendChild(card);
    });
  } catch (error) {
    console.error(error);
    customersList.innerHTML = '<p class="placeholder">Erro ao carregar clientes.</p>';
  }
};

// Formulário de Chamado
const ticketForm = $("#ticket-form");
const budgetModal = $("#budget-modal");
const budgetForm = $("#budget-form");
const budgetClose = $("#budget-close");
const laudoForm = $("#laudo-form");
const laudoTicketInput = $("#laudo-ticket-id");
const laudoTechnicianInput = $("#laudo-technician");
const inventoryReportBtn = $("#inventory-report-btn");
const gotoCustomersBtn = $("#goto-customers-btn");
let ticketsCache = [];

const toggleModal = (open) => {
  if (open) {
    budgetModal.classList.add("modal--open");
    budgetModal.setAttribute("aria-hidden", "false");
  } else {
    budgetModal.classList.remove("modal--open");
    budgetModal.setAttribute("aria-hidden", "true");
    budgetForm.reset();
  }
};

const openBudgetModal = (ticketId) => {
  $("#budget-ticket-id").value = ticketId;
  toggleModal(true);
};

budgetClose.addEventListener("click", () => toggleModal(false));
budgetModal.addEventListener("click", (event) => {
  if (event.target === budgetModal) {
    toggleModal(false);
  }
});
ticketForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  
  if (!selectedCustomer) {
    showToast("Por favor, selecione um cliente primeiro");
    return;
  }
  
  const formData = new FormData(ticketForm);
  
  const ticketData = {
    customerId: selectedCustomer.id,
    deviceType: formData.get("deviceType"),
    deviceModel: formData.get("device"),
    description: formData.get("description"),
  };
  
  try {
    const response = await fetch("/api/tickets", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(ticketData),
    });
    
    if (!response.ok) {
      throw new Error(await extractError(response));
    }
    
    showToast("Chamado registrado com sucesso!");
    ticketForm.reset();
    selectedCustomer = null;
    selectedCustomerDiv.style.display = "none";
    customerSearch.disabled = false;
    loadTickets();
    loadDashboard();
  } catch (error) {
    showToast("Erro ao registrar chamado: " + error.message);
  }
});

const loadTickets = async () => {
  const ticketsList = $("#tickets-list");
  ticketsList.innerHTML = '<p class="placeholder">Carregando...</p>';
  
  try {
    const response = await fetch("/api/tickets");
    if (!response.ok) throw new Error(await extractError(response));
    
    const tickets = await response.json();
    ticketsCache = tickets;
    
    if (tickets.length === 0) {
      ticketsList.innerHTML = '<p class="placeholder">Nenhum chamado registrado ainda.</p>';
      return;
    }
    
    ticketsList.innerHTML = "";
    tickets.forEach((ticket) => {
      const item = document.createElement("div");
      item.className = "ticket-item";
      item.innerHTML = `
        <div class="ticket-item__header">
          <div>
            <div class="ticket-item__id">${ticket.id}</div>
            <div class="ticket-item__customer">${ticket.customerName}</div>
          </div>
          <button class="btn btn--ghost ticket-toggle" type="button">Ver detalhes</button>
        </div>
        <div class="ticket-item__meta">
          <span class="ticket-item__device">${ticket.device}</span>
          <span class="ticket-item__date">Entrada: ${ticket.entryDate}</span>
          <span class="pill pill--info">${statusLabel[ticket.status]}</span>
        </div>
        <div class="ticket-item__details">
          <p class="ticket-item__description"><strong>Queixa do cliente:</strong> ${ticket.description || "Sem descrição"}</p>
          <div class="ticket-item__actions">
            <button class="btn btn--primary ticket-action" data-action="budget" data-ticket="${ticket.id}">Gerar orçamento sem compromisso</button>
            <button class="btn btn--secondary ticket-action" data-action="laudo" data-ticket="${ticket.id}">Gerar laudo</button>
          </div>
        </div>
      `;
      const toggleButton = item.querySelector(".ticket-toggle");
      const details = item.querySelector(".ticket-item__details");
      toggleButton.addEventListener("click", () => {
        details.classList.toggle("open");
        toggleButton.textContent = details.classList.contains("open") ? "Ocultar detalhes" : "Ver detalhes";
      });
      item.querySelectorAll(".ticket-action").forEach((button) => {
        button.addEventListener("click", (event) => handleTicketAction(event));
      });
      ticketsList.appendChild(item);
    });
  } catch (error) {
    console.error(error);
    ticketsList.innerHTML = '<p class="placeholder">Erro ao carregar chamados.</p>';
  }
};

// Gerenciamento de Estoque
const inventoryForm = $("#inventory-form");
const inventoryTbody = $("#inventory-tbody");

inventoryForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  const formData = new FormData(inventoryForm);
  
  const itemData = {
    type: formData.get("type"),
    brand: formData.get("brand"),
    quantity: parseInt(formData.get("quantity"), 10),
    price: parseFloat(formData.get("price")),
  };
  
  try {
    const response = await fetch("/api/inventory", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(itemData),
    });
    
    if (!response.ok) {
      throw new Error(await extractError(response));
    }
    
    showToast("Peça registrada com sucesso!");
    inventoryForm.reset();
    loadInventory();
  } catch (error) {
    showToast("Erro ao registrar peça: " + error.message);
  }
});

const loadInventory = async () => {
    inventoryTbody.innerHTML = '<tr><td colspan="5" class="placeholder">Carregando...</td></tr>';
  
  try {
    const response = await fetch("/api/inventory");
    if (!response.ok) throw new Error(await extractError(response));
    
    const items = await response.json();
    
    if (items.length === 0) {
      inventoryTbody.innerHTML = '<tr><td colspan="5" class="placeholder">Nenhuma peça cadastrada ainda.</td></tr>';
      return;
    }
    
    inventoryTbody.innerHTML = "";
    items.forEach((item) => {
      const row = document.createElement("tr");
      row.innerHTML = `
        <td><strong style="color: var(--primary);">${item.id}</strong></td>
        <td>${item.type}</td>
        <td>${item.brand}</td>
        <td><strong>${item.quantity}</strong></td>
        <td>${formatCurrency(item.price)}</td>
      `;
      inventoryTbody.appendChild(row);
    });
  } catch (error) {
    console.error(error);
    inventoryTbody.innerHTML = '<tr><td colspan="5" class="placeholder">Erro ao carregar estoque.</td></tr>';
  }
};

// Toast
const showToast = (message) => {
  const toast = document.createElement("div");
  toast.textContent = message;
  toast.style.position = "fixed";
  toast.style.bottom = "2rem";
  toast.style.right = "2rem";
  toast.style.background = "#ff8c5a";
  toast.style.color = "white";
  toast.style.padding = "0.75rem 1.25rem";
  toast.style.borderRadius = "999px";
  toast.style.boxShadow = "0 15px 35px rgba(255, 140, 90, 0.4)";
  toast.style.zIndex = "100";
  document.body.appendChild(toast);
  setTimeout(() => toast.remove(), 3000);
};

// Navegar para página de chamados com cliente pré-selecionado
const navigateToTicketsWithCustomer = async (customer) => {
  // Navegar para página de chamados
  navigateToPage("tickets");
  
  // Aguardar um pouco para garantir que a página foi renderizada
  setTimeout(async () => {
    try {
      // Buscar o cliente completo pela API para garantir que temos todos os dados
      const response = await fetch(`/api/customers/search?query=${encodeURIComponent(customer.id)}`);
      if (response.ok) {
        const customers = await response.json();
        const foundCustomer = customers.find(c => c.id === customer.id);
        if (foundCustomer) {
          selectCustomer(foundCustomer);
          // Scroll para o formulário de chamado
          ticketForm.scrollIntoView({ behavior: "smooth", block: "start" });
        }
      }
    } catch (error) {
      console.error("Erro ao pré-selecionar cliente:", error);
    }
  }, 100);
};

// Navegação rápida entre páginas
const navigateToPage = (pageName) => {
  navLinks.forEach((link) => {
    link.classList.remove("active");
    if (link.dataset.page === pageName) {
      link.classList.add("active");
    }
  });
  
  pages.forEach((page) => page.classList.remove("active"));
  $(`#page-${pageName}`).classList.add("active");
  
  // Carregar dados da página
  if (pageName === "home") {
    loadDashboard();
  } else if (pageName === "tickets") {
    loadTickets();
  } else if (pageName === "customers") {
    loadCustomers();
  } else if (pageName === "inventory") {
    loadInventory();
  } else if (pageName === "laudo") {
    loadLaudoTickets();
  }
};

if (gotoCustomersBtn) {
  gotoCustomersBtn.addEventListener("click", () => {
    navigateToPage("customers");
  });
}

// Inicialização
loadDashboard();

const formatCurrency = (value) => {
  if (value === null || value === undefined) return "R$ 0,00";
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(value);
};

const handleTicketAction = (event) => {
  const action = event.target.dataset.action;
  const ticketId = event.target.dataset.ticket;
  if (action === "budget") {
    openBudgetModal(ticketId);
  } else if (action === "laudo") {
    navigateToLaudo(ticketId);
  }
};

const navigateToLaudo = async (ticketId) => {
  // Atualizar links ativos
  navLinks.forEach((link) => {
    link.classList.remove("active");
    if (link.dataset.page === "laudo") {
      link.classList.add("active");
    }
  });
  
  // Mostrar página correta
  pages.forEach((page) => page.classList.remove("active"));
  $("#page-laudo").classList.add("active");
  
  await loadLaudoTickets();
  
  if (ticketId) {
    laudoTicketInput.value = ticketId;
  }
  
  laudoTechnicianInput.focus();
};

const loadLaudoTickets = async () => {
  const select = laudoTicketInput;
  select.innerHTML = '<option value="">Carregando chamados...</option>';
  select.disabled = true;
  
  try {
    const response = await fetch("/api/tickets");
    if (!response.ok) throw new Error(await extractError(response));
    
    const tickets = await response.json();
    
    // Filtrar apenas chamados em aberto (não finalizados)
    const openTickets = tickets.filter(ticket => ticket.status !== "FINALIZADO");
    
    if (openTickets.length === 0) {
      select.innerHTML = '<option value="">Nenhum chamado em aberto disponível</option>';
      select.disabled = true;
      return;
    }
    
    select.innerHTML = '<option value="">Selecione um chamado</option>';
    openTickets.forEach((ticket) => {
      const option = document.createElement("option");
      option.value = ticket.id;
      option.textContent = `${ticket.id} - ${ticket.customerName} - ${ticket.device} (${statusLabel[ticket.status]})`;
      select.appendChild(option);
    });
    
    select.disabled = false;
  } catch (error) {
    console.error(error);
    select.innerHTML = '<option value="">Erro ao carregar chamados</option>';
    select.disabled = true;
  }
};

budgetForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const formData = new FormData(budgetForm);
  const ticketId = formData.get("ticketId");

  const payload = {
    diagnosis: formData.get("diagnosis"),
    cause: formData.get("cause"),
    equipmentHasConsent: formData.get("equipmentHasConsent") === "on",
    powerSupplyAffected: formData.get("powerSupplyAffected") === "on",
    part: {
      name: formData.get("partName"),
      specification: formData.get("partSpecification"),
      code: formData.get("partCode"),
      partPrice: parseFloat(formData.get("partPrice")) || 0,
      laborPrice: parseFloat(formData.get("laborPrice")) || 0,
    },
    laborInfo: {
      description: formData.get("laborDescription"),
      estimatedTime: formData.get("laborTime"),
      responsible: formData.get("laborResponsible"),
    },
  };

  try {
    const response = await fetch(`/api/tickets/${ticketId}/budget`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    if (!response.ok) {
      throw new Error(await extractError(response));
    }
    const blob = await response.blob();
    downloadPdf(blob, `orcamento-${ticketId}.pdf`);
    toggleModal(false);
    showToast("Orçamento gerado com sucesso!");
  } catch (error) {
    showToast(error.message);
  }
});

laudoForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const formData = new FormData(laudoForm);
  const ticketId = formData.get("ticketId");
  const payload = {
    equipmentCondition: formData.get("equipmentCondition"),
    problemDescription: formData.get("problemDescription"),
    diagnostic: formData.get("diagnostic"),
    actionsTaken: formData.get("actionsTaken"),
    recommendations: formData.get("recommendations"),
    technicianName: formData.get("technicianName"),
  };

  try {
    const response = await fetch(`/api/tickets/${ticketId}/laudo`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    if (!response.ok) {
      throw new Error(await extractError(response));
    }
    const blob = await response.blob();
    downloadPdf(blob, `laudo-${ticketId}.pdf`);
    showToast("Laudo gerado com sucesso!");
  } catch (error) {
    showToast(error.message);
  }
});

const downloadPdf = (blob, filename) => {
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
};

inventoryReportBtn.addEventListener("click", async () => {
  try {
    const response = await fetch("/api/inventory/report");
    if (!response.ok) throw new Error(await extractError(response));
    const blob = await response.blob();
    downloadPdf(blob, "relatorio-estoque.pdf");
    showToast("Relatório de estoque gerado!");
  } catch (error) {
    showToast(error.message);
  }
});

const extractError = async (response) => {
  try {
    const data = await response.json();
    if (data?.message) {
      const detail = Array.isArray(data.details) && data.details.length ? `: ${data.details[0]}` : "";
      return `${data.message}${detail}`;
    }
  } catch {
    // ignore parsing errors
  }
  return response.statusText || "Erro ao processar requisição";
};
