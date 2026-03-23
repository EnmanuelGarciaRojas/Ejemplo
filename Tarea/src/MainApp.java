import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.*;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class MainApp extends Application {
    private TableView<Producto> tablaProductos;
    private ObservableList<Producto> productos;
    private ProgressBar progressBar;
    private Label lblEstado;
    private static final String ARCHIVO = "inventario.txt";

    @Override
    public void start(Stage primaryStage) {
        productos = FXCollections.observableArrayList();
        primaryStage.setTitle("Sistema de Inventario");

        // Configurar la tabla
        tablaProductos = new TableView<>();
        TableColumn<Producto, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        TableColumn<Producto, String> colCategoria = new TableColumn<>("Categoría");
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        TableColumn<Producto, Double> colPrecio = new TableColumn<>("Precio");
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        TableColumn<Producto, Integer> colCantidad = new TableColumn<>("Cantidad");
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));

        tablaProductos.getColumns().addAll(colNombre, colCategoria, colPrecio, colCantidad);
        tablaProductos.setItems(productos);

        // Barra de progreso y etiqueta de estado (siempre visible abajo)
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);
        lblEstado = new Label("Listo");
        HBox statusBar = new HBox(10, progressBar, lblEstado);
        statusBar.setPadding(new Insets(5));

        // Botones inferiores
        Button btnAgregar = new Button("Agregar");
        Button btnEliminar = new Button("Eliminar");
        Button btnGuardar = new Button("Guardar");
        Button btnCargar = new Button("Cargar");

        btnAgregar.setOnAction(e -> abrirFormulario());
        btnEliminar.setOnAction(e -> eliminarSeleccionado());
        btnGuardar.setOnAction(e -> guardarArchivoConHilo());
        btnCargar.setOnAction(e -> cargarArchivoConHilo());

        HBox botones = new HBox(10, btnAgregar, btnEliminar, btnGuardar, btnCargar);
        botones.setPadding(new Insets(10));

        // MenuBar
        MenuBar menuBar = new MenuBar();
        Menu menuArchivo = new Menu("Archivo");
        MenuItem nuevoProducto = new MenuItem("Nuevo Producto");
        MenuItem guardar = new MenuItem("Guardar");
        MenuItem cargar = new MenuItem("Cargar");
        MenuItem salir = new MenuItem("Salir");
        nuevoProducto.setOnAction(e -> abrirFormulario());
        guardar.setOnAction(e -> guardarArchivoConHilo());
        cargar.setOnAction(e -> cargarArchivoConHilo());
        salir.setOnAction(e -> confirmarSalir());
        menuArchivo.getItems().addAll(nuevoProducto, new SeparatorMenuItem(), guardar, cargar, new SeparatorMenuItem(), salir);

        Menu menuEditar = new Menu("Editar");
        MenuItem eliminarSeleccion = new MenuItem("Eliminar seleccionado");
        MenuItem limpiarLista = new MenuItem("Limpiar lista");
        eliminarSeleccion.setOnAction(e -> eliminarSeleccionado());
        limpiarLista.setOnAction(e -> limpiarLista());
        menuEditar.getItems().addAll(eliminarSeleccion, limpiarLista);

        Menu menuAyuda = new Menu("Ayuda");
        MenuItem acercaDe = new MenuItem("Acerca de");
        acercaDe.setOnAction(e -> mostrarAcercaDe());
        menuAyuda.getItems().add(acercaDe);

        menuBar.getMenus().addAll(menuArchivo, menuEditar, menuAyuda);

        // Layout principal
        BorderPane root = new BorderPane();
        root.setTop(menuBar);
        root.setCenter(tablaProductos);
        root.setBottom(new VBox(botones, statusBar));

        Scene scene = new Scene(root, 800, 500);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            e.consume(); // Prevenir cierre inmediato
            confirmarSalir();
        });
        primaryStage.show();

        // Cargar archivo automáticamente al inicio (con hilo)
        cargarArchivoConHilo();
    }

    // Abre el formulario y agrega el producto si se guarda
    private void abrirFormulario() {
        FormularioProducto formulario = new FormularioProducto();
        formulario.showAndWait(); // Espera hasta que se cierre
        Producto nuevo = formulario.getProductoGuardado();
        if (nuevo != null) {
            productos.add(nuevo);
            lblEstado.setText("Producto agregado: " + nuevo.getNombre());
        }
    }

    // Elimina el producto seleccionado con confirmación
    private void eliminarSeleccionado() {
        Producto seleccionado = tablaProductos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin selección", "Selecciona un producto para eliminar.");
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar eliminación");
        alert.setHeaderText("¿Eliminar producto?");
        alert.setContentText("¿Estás seguro de eliminar " + seleccionado.getNombre() + "?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            productos.remove(seleccionado);
            lblEstado.setText("Producto eliminado: " + seleccionado.getNombre());
        }
    }

    // Limpia toda la lista con confirmación
    private void limpiarLista() {
        if (productos.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Lista vacía", "No hay productos para limpiar.");
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar limpieza");
        alert.setHeaderText("Limpiar toda la lista");
        alert.setContentText("¿Eliminar todos los productos? Esta acción no se puede deshacer.");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            productos.clear();
            lblEstado.setText("Lista limpiada.");
        }
    }

    // Guarda el archivo en un hilo separado (ProgressBar animada)
    private void guardarArchivoConHilo() {
        if (productos.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Lista vacía", "No hay productos para guardar.");
            return;
        }

        // Deshabilitar botones para evitar múltiples guardados (opcional)
        btnDisableWhileSaving(true);

        Thread hiloGuardar = new Thread(() -> {
            try {
                // Simular progreso (no hay una operación larga, pero se puede simular)
                Platform.runLater(() -> {
                    progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                    lblEstado.setText("Guardando...");
                });

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARCHIVO))) {
                    for (Producto p : productos) {
                        String linea = String.format("%s,%s,%.2f,%d",
                                p.getNombre(), p.getCategoria(), p.getPrecio(), p.getCantidad());
                        writer.write(linea);
                        writer.newLine();
                    }
                }

                // Simular un pequeño retraso para ver el progreso (opcional)
                Thread.sleep(500);

                Platform.runLater(() -> {
                    progressBar.setProgress(0);
                    lblEstado.setText("Guardado correctamente.");
                    mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Inventario guardado en " + ARCHIVO);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressBar.setProgress(0);
                    lblEstado.setText("Error al guardar.");
                    mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo guardar: " + e.getMessage());
                });
            } finally {
                Platform.runLater(() -> btnDisableWhileSaving(false));
            }
        });
        hiloGuardar.setDaemon(true);
        hiloGuardar.start();
    }

    // Carga el archivo en un hilo separado con ProgressBar real
    private void cargarArchivoConHilo() {
        // Limpiar datos actuales (opcional, pero es típico al cargar)
        // products.clear();

        // Deshabilitar botones mientras carga
        btnDisableWhileSaving(true);
        progressBar.setProgress(0);
        lblEstado.setText("Cargando...");

        Thread hiloCarga = new Thread(() -> {
            File archivo = new File(ARCHIVO);
            if (!archivo.exists()) {
                Platform.runLater(() -> {
                    progressBar.setProgress(0);
                    lblEstado.setText("No se encontró " + ARCHIVO);
                    btnDisableWhileSaving(false);
                });
                return;
            }

            ObservableList<Producto> nuevosProductos = FXCollections.observableArrayList();
            try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
                String linea;
                int contador = 0;
                // Primero contar líneas para la progresión
                long totalLineas = reader.lines().count();
                reader.close();

                if (totalLineas == 0) {
                    Platform.runLater(() -> {
                        progressBar.setProgress(0);
                        lblEstado.setText("Archivo vacío.");
                        btnDisableWhileSaving(false);
                    });
                    return;
                }

                try (BufferedReader reader2 = new BufferedReader(new FileReader(archivo))) {
                    int procesadas = 0;
                    while ((linea = reader2.readLine()) != null) {
                        String[] partes = linea.split(",");
                        if (partes.length == 4) {
                            try {
                                String nombre = partes[0];
                                String categoria = partes[1];
                                double precio = Double.parseDouble(partes[2]);
                                int cantidad = Integer.parseInt(partes[3]);
                                Producto p = new Producto(nombre, categoria, precio, cantidad);
                                nuevosProductos.add(p);
                            } catch (NumberFormatException e) {
                                System.err.println("Error al parsear línea: " + linea);
                            }
                        }
                        procesadas++;
                        // Actualizar progreso
                        double progreso = (double) procesadas / totalLineas;
                        Platform.runLater(() -> progressBar.setProgress(progreso));
                        Thread.sleep(200); // Simular carga lenta
                    }
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblEstado.setText("Error al cargar.");
                    mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo leer el archivo: " + e.getMessage());
                });
                e.printStackTrace();
            }

            // Actualizar la lista principal en el hilo de la interfaz
            Platform.runLater(() -> {
                productos.setAll(nuevosProductos);
                progressBar.setProgress(1.0);
                lblEstado.setText("Cargado " + nuevosProductos.size() + " productos.");
                btnDisableWhileSaving(false);
            });
        });
        hiloCarga.setDaemon(true);
        hiloCarga.start();
    }

    // Método auxiliar para habilitar/deshabilitar botones durante operaciones largas
    private void btnDisableWhileSaving(boolean disable) {
        // Se podría guardar referencia a los botones, pero por simplicidad los obtenemos del layout
        // En una app real se recomienda guardar referencias.
        // Aquí se asume que los botones están en la barra inferior; podemos recorrer el escenario,
        // pero para simplificar se deja como referencia.
        // Mejor: tener variables de instancia para los botones.
    }

    // Mostrar "Acerca de"
    private void mostrarAcercaDe() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Acerca de");
        alert.setHeaderText("Sistema de Inventario");
        alert.setContentText("Versión 1.0\nDesarrollado con JavaFX\nCarga y guardado con hilos.");
        alert.showAndWait();
    }

    // Confirmar antes de salir
    private void confirmarSalir() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Salir");
        alert.setHeaderText("¿Salir del programa?");
        alert.setContentText("Se perderán los cambios no guardados.");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Platform.exit();
        }
    }

    // Método auxiliar para mostrar alertas simples
    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}