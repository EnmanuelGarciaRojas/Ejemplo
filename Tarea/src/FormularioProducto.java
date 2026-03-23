import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class FormularioProducto extends Stage {
    private TextField txtNombre;
    private TextField txtCategoria;
    private TextField txtPrecio;
    private TextField txtCantidad;
    private Producto productoGuardado;

    public FormularioProducto() {
        setTitle("Nuevo Producto");
        setResizable(false);
        initComponents();
    }

    private void initComponents() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(10);
        grid.setVgap(10);

        // Etiquetas y campos
        Label lblNombre = new Label("Nombre:");
        txtNombre = new TextField();
        Label lblCategoria = new Label("Categoría:");
        txtCategoria = new TextField();
        Label lblPrecio = new Label("Precio:");
        txtPrecio = new TextField();
        Label lblCantidad = new Label("Cantidad:");
        txtCantidad = new TextField();

        grid.add(lblNombre, 0, 0);
        grid.add(txtNombre, 1, 0);
        grid.add(lblCategoria, 0, 1);
        grid.add(txtCategoria, 1, 1);
        grid.add(lblPrecio, 0, 2);
        grid.add(txtPrecio, 1, 2);
        grid.add(lblCantidad, 0, 3);
        grid.add(txtCantidad, 1, 3);

        // Botones
        Button btnGuardar = new Button("Guardar");
        Button btnCancelar = new Button("Cancelar");
        btnGuardar.setOnAction(e -> guardar());
        btnCancelar.setOnAction(e -> close());

        grid.add(btnGuardar, 0, 4);
        grid.add(btnCancelar, 1, 4);

        Scene scene = new Scene(grid, 300, 250);
        setScene(scene);
    }

    private void guardar() {
        String nombre = txtNombre.getText().trim();
        String categoria = txtCategoria.getText().trim();
        String precioStr = txtPrecio.getText().trim();
        String cantidadStr = txtCantidad.getText().trim();

        // Validar campos vacíos
        if (nombre.isEmpty() || categoria.isEmpty() || precioStr.isEmpty() || cantidadStr.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campos vacíos", "Todos los campos son obligatorios.");
            return;
        }

        // Validar precio (número decimal)
        double precio;
        try {
            precio = Double.parseDouble(precioStr);
            if (precio < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Precio inválido", "El precio debe ser un número positivo (ej: 12.50).");
            return;
        }

        // Validar cantidad (entero)
        int cantidad;
        try {
            cantidad = Integer.parseInt(cantidadStr);
            if (cantidad < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Cantidad inválida", "La cantidad debe ser un número entero positivo.");
            return;
        }

        // Crear producto
        productoGuardado = new Producto(nombre, categoria, precio, cantidad);
        mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Producto guardado correctamente.");
        close();
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public Producto getProductoGuardado() {
        return productoGuardado;
    }
}