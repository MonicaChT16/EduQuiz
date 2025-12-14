const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');

// ==========================================
// 1. CONFIGURACIÓN Y CONEXIÓN
// ==========================================

// Ajusta esta ruta si tu llave está en otro lado. 
// '..' significa "baja una carpeta" (asumiendo que el script está en /scripts y la llave en la raíz)
const serviceAccountPath = path.join(__dirname, '..', 'serviceAccountKey.json');

// Verificar credenciales
if (!fs.existsSync(serviceAccountPath)) {
    console.error('❌ ERROR: No se encontró serviceAccountKey.json');
    console.error('   Ruta buscada:', serviceAccountPath);
    process.exit(1);
}

// Inicializar Firebase
try {
    const serviceAccount = require(serviceAccountPath);
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
    });
    console.log('✅ Firebase conectado correctamente');
} catch (error) {
    console.error('❌ Error de conexión:', error.message);
    process.exit(1);
}

const db = admin.firestore();

// ==========================================
// 2. LÓGICA DE CARGA DE DATOS
// ==========================================

async function initFirestore() {
    console.log('🚀 Iniciando carga de datos Pack 1 (Real)...');

    try {
        // Test de conexión
        await db.collection('_system').doc('init').set({
            initialized: true,
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            version: '1.0.0'
        });

        // --- DATOS DEL PACK 2025-W01 ---
        const now = Date.now();
        const packId = 'pack_2025_w01';
        const weekLabel = '2025-W01';

        // 1. Crear Pack
        console.log('\n📦 Creando Pack...');
        const packRef = db.collection('packs').doc(packId);
        await packRef.set({
            packId: packId,
            weekLabel: weekLabel,
            status: 'PUBLISHED',
            publishedAt: now,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
        });

        // 2. Definir Textos
        const texts = [
            // --- LECTURA ---
            {
                textId: 'txt_2025_w01_read_01', packId: packId, subject: 'LECTURA', title: 'El Regalo',
                body: `Cuando la mujer vio la casa pasar río abajo, pensó que sabía de quién era. Había sido horrible verla pasar a la deriva, pero sus dueños debían haber escapado a tierras más altas. Más tarde, con la lluvia y la oscuridad cercándola, oyó río arriba el rugido de una pantera. Ahora, la casa parecía temblar como algo vivo que la rodeaba. Ella se aferró al borde de la cama. Balanceándose de un lado a otro, su propia casa se movió hasta donde dio la amarra. Hubo una sacudida y un quejido de maderas viejas y, luego, una pausa. Lentamente, la corriente la soltó y dejó que se balanceara hacia atrás, arrastrándola hasta su ubicación inicial. En algún momento de la noche, un grito la despertó. En la oscuridad, oyó algo que se movía afuera, algo grande que hacía un ruido como un barco excavador. Ahora sabía qué era eso: un enorme felino que un árbol arrancado de raíz le dejó al pasar. Había llegado con la inundación: un regalo. La mujer estaba muerta de hambre, llevaba días sin comer. El rifle se movió sobre sus rodillas. Inconscientemente, apretó una mano contra su cara. La lluvia todavía caía como si no fuese a parar nunca.`
            },
            {
                textId: 'txt_2025_w01_read_02', packId: packId, subject: 'LECTURA', title: 'Graffiti',
                body: `Se presentan dos cartas enviadas por lectores a un periódico sobre el tema de los grafitis:\n\nCarta de Helga: "Estoy hirviendo de rabia mientras limpian y pintan por cuarta vez la pared de la escuela para borrar los grafitis. La creatividad es admirable, pero la gente debería encontrar maneras de expresarse que no causaran costos adicionales a la sociedad. ¿Por qué estropear la reputación de los jóvenes pintando en lugares prohibidos? Los artistas profesionales no cuelgan sus cuadros en las calles, ¿verdad? Lo que hacen es buscar financiación y ganar fama a través de exposiciones legales."\n\nCarta de Sofía: "Sobre los gustos no hay nada escrito. La sociedad está llena de comunicación visual. ¿Acaso los que instalan las vallas publicitarias gigantes te han pedido permiso para ponerlas frente a tu vista? No. Entonces, ¿deberían hacerlo los pintores de graffiti? ¿No es todo una cuestión de comunicación? Piensa en la ropa de rayas y cuadros que apareció hace algunos años en las tiendas. El estampado y los colores los habían copiado directamente de las floridas pintadas que llenaban los muros de cemento. Es bastante chocante que aceptemos y admiremos estos estampados en la ropa y que, en cambio, ese mismo estilo en graffiti nos parezca horroroso."`
            },
            {
                textId: 'txt_2025_w01_read_03', packId: packId, subject: 'LECTURA', title: 'El Lago Chad',
                body: `El Lago Chad, situado en el norte de África, ha sufrido cambios drásticos en su nivel de agua a lo largo de la historia. Los estudios geológicos muestran la siguiente cronología aproximada:\n• Año 20.000 a.C.: El lago desapareció por completo debido a las condiciones climáticas de la última era glacial.\n• Año 11.000 a.C.: El lago reapareció y comenzó a subir de nivel.\n• Año 4.000 a.C.: El lago alcanzó su nivel máximo histórico, siendo mucho más profundo que hoy.\n• Año 1.000 d.C. hasta Hoy: El nivel del lago ha descendido y actualmente es aproximadamente el mismo que tenía en el año 1.000 d.C.\n\nPor otro lado, el arte rupestre del Sahara (dibujos en rocas) muestra qué animales vivían en la zona en diferentes épocas. Por ejemplo, los dibujos de rinocerontes e hipopótamos son muy antiguos, mientras que los dibujos de camellos son mucho más recientes.`
            },
            // --- CIENCIAS ---
            { textId: 'txt_2025_w01_sci_01', packId: packId, subject: 'CIENCIAS', title: 'Antibióticos y Bacterias', body: 'Una persona tiene una infección bacteriana. El médico le prescribió un tratamiento con antibióticos por 7 días. Después de 3 días, la persona se siente mucho mejor y decide dejar de tomar las pastillas para no "meterle químicos" al cuerpo.' },
            { textId: 'txt_2025_w01_sci_02', packId: packId, subject: 'CIENCIAS', title: 'La Fotosíntesis y la Luz', body: 'Un estudiante coloca una planta acuática bajo una lámpara y observa que se forman pequeñas burbujas que suben a la superficie del agua.' },
            { textId: 'txt_2025_w01_sci_03', packId: packId, subject: 'CIENCIAS', title: 'Vacunas y Memoria', body: 'Las vacunas contienen fragmentos de virus o bacterias debilitados o muertos que se introducen en el cuerpo humano.' },
            { textId: 'txt_2025_w01_sci_04', packId: packId, subject: 'CIENCIAS', title: 'El Ecosistema del Estanque', body: 'En un estanque, la población de ranas disminuye drásticamente debido a una enfermedad. Las ranas se alimentan principalmente de mosquitos, y las serpientes del estanque se alimentan de ranas.' },
            { textId: 'txt_2025_w01_sci_05', packId: packId, subject: 'CIENCIAS', title: 'Ejercicio y Frecuencia', body: 'Durante una carrera de 100 metros, la frecuencia cardíaca y la frecuencia respiratoria de un atleta aumentan considerablemente.' },
            { textId: 'txt_2025_w01_sci_06', packId: packId, subject: 'CIENCIAS', title: 'Digestión de Almidón', body: 'La saliva contiene una enzima llamada amilasa. Si mantienes un trozo de pan (rico en almidón) en la boca por mucho tiempo sin tragarlo, empezarás a sentir un sabor dulce.' },
            { textId: 'txt_2025_w01_sci_07', packId: packId, subject: 'CIENCIAS', title: 'Selección Natural', body: 'En una población de mariposas blancas que viven en un bosque de árboles de corteza clara, aparece una mutación que las hace oscuras. Debido a la contaminación industrial, la corteza de los árboles se vuelve negra por el hollín.' },
            { textId: 'txt_2025_w01_sci_08', packId: packId, subject: 'CIENCIAS', title: 'Diabetes y Páncreas', body: 'La insulina es una hormona producida por el páncreas que regula los niveles de glucosa en la sangre. En la Diabetes Tipo 1, el páncreas no produce insulina.' },
            { textId: 'txt_2025_w01_sci_09', packId: packId, subject: 'CIENCIAS', title: 'Herencia Genética', body: 'En los humanos, el alelo para el lóbulo de la oreja separado (D) es dominante sobre el lóbulo pegado (d). Un padre tiene genotipo Dd y la madre dd.' },
            { textId: 'txt_2025_w01_sci_10', packId: packId, subject: 'CIENCIAS', title: 'El Sudor', body: 'Cuando realizamos actividad física en un día caluroso, nuestro cuerpo produce sudor.' },
            // --- MATEMÁTICAS ---
            { textId: 'txt_2025_w01_math_01', packId: packId, subject: 'MATEMATICA', title: 'Pasos de Enrique', body: 'La longitud del paso P es la distancia entre los extremos posteriores de dos huellas consecutivas. Para los hombres, la fórmula n/p = 140 da una relación aproximada entre n y p donde: n = número de pasos por minuto. p = longitud del paso en metros.' },
            { textId: 'txt_2025_w01_math_02', packId: packId, subject: 'MATEMATICA', title: 'El Tipo de Cambio', body: 'Mei-Ling se enteró de que el tipo de cambio entre el dólar de Singapur y el rand sudafricano era de: 1 SGD = 4,2 ZAR. Mei-Ling cambió 3 000 dólares de Singapur en rands sudafricanos con este tipo de cambio.' },
            { textId: 'txt_2025_w01_math_03', packId: packId, subject: 'MATEMATICA', title: 'Notas de Irene', body: 'En el colegio de Irene, su profesora de ciencias les hace exámenes que se puntúan de 0 a 100. Irene tiene una media de 60 puntos de sus primeros cuatro exámenes de ciencias. En el quinto examen sacó 80 puntos.' },
            { textId: 'txt_2025_w01_math_04', packId: packId, subject: 'MATEMATICA', title: 'Repisas del Carpintero', body: 'Para construir repisas, un carpintero necesita lo siguiente: 4 tablas largas de madera, 6 tablas cortas de madera, 12 ganchos pequeños, 2 ganchos grandes, 14 tornillos. El carpintero tiene en el almacén 26 tablas largas de madera, 33 tablas cortas de madera, 200 ganchos pequeños, 20 ganchos grandes y 510 tornillos.' },
            { textId: 'txt_2025_w01_math_05', packId: packId, subject: 'MATEMATICA', title: 'Ingredientes de la Pizza', body: 'En una pizzería se puede elegir una pizza básica con dos ingredientes: queso y tomate. También puedes diseñar tu propia pizza con ingredientes adicionales. Se puede seleccionar entre cuatro ingredientes adicionales diferentes: aceitunas, jamón, champiñones y salami. Jaime quiere encargar una pizza con dos ingredientes adicionales diferentes.' },
            { textId: 'txt_2025_w01_math_06', packId: packId, subject: 'MATEMATICA', title: 'Vuelo Espacial', body: 'La Mir daba vueltas alrededor de la Tierra a una altura aproximada de 400 km. El diámetro de la Tierra mide aproximadamente 12 700 km y su circunferencia es de alrededor de 40 000 km. Se pide calcular la distancia aproximada recorrida en 86 500 vueltas.' },
            { textId: 'txt_2025_w01_math_07', packId: packId, subject: 'MATEMATICA', title: 'Esquema de Escalera', body: 'Roberto construye un esquema de una escalera usando cuadrados. Utiliza un cuadrado para la Etapa 1, tres cuadrados para la Etapa 2, y seis para la Etapa 3.' },
            { textId: 'txt_2025_w01_math_08', packId: packId, subject: 'MATEMATICA', title: 'Manzanas y Pinos', body: 'Hay dos fórmulas que puedes usar para calcular la cantidad de árboles de manzana y de pino en un patrón descrito: Número de árboles de manzana = n * n. Número de pinos = 8 * n. Donde n es el número de filas de árboles de manzana.' },
            { textId: 'txt_2025_w01_math_09', packId: packId, subject: 'MATEMATICA', title: 'Pizzas', body: 'Una pizzería ofrece dos pizzas redondas del mismo grosor, pero de diferentes tamaños. La pequeña tiene un diámetro de 30 cm y cuesta 30 zeds. La grande tiene un diámetro de 40 cm y cuesta 40 zeds.' },
            { textId: 'txt_2025_w01_math_10', packId: packId, subject: 'MATEMATICA', title: 'Frecuencia Cardíaca', body: 'Por razones de salud la gente debería limitar sus esfuerzos, al hacer deporte, por ejemplo, para no superar una determinada frecuencia cardíaca. Durante años la relación entre la máxima frecuencia cardíaca recomendada para una persona y su edad se describía mediante la fórmula: 220 - edad. Investigaciones recientes sugieren una nueva fórmula: 208 - (0.7 * edad). Un artículo afirma que con la nueva fórmula, el máximo recomendado disminuye para los jóvenes y aumenta para los mayores.' }
        ];

        console.log('\n📚 Subiendo Textos...');
        for (const text of texts) {
            await db.collection('texts').doc(text.textId).set({
                ...text,
                gradeBand: 'PISA',
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
                updatedAt: admin.firestore.FieldValue.serverTimestamp(),
                status: 'PUBLISHED'
            });
        }

        // 3. Crear Preguntas
        console.log('\n❓ Creando preguntas...');
        const questions = [
            // === LECTURA ===
            { questionId: 'q_2025_w01_read_01_01', textId: 'txt_2025_w01_read_01', packId: packId, prompt: '¿Cuál es la situación de la mujer al comienzo del relato?', correctOptionId: 'C', difficulty: 2, explanationText: 'El texto describe una inundación severa. La mujer está sola en su casa, rodeada de agua y oscuridad, lo que confirma su aislamiento por la crecida del río.', options: [{optionId: 'A', text: 'Está esperando a que lleguen unos visitantes en barco.'}, {optionId: 'B', text: 'Se está defendiendo de un animal salvaje.'}, {optionId: 'C', text: 'Su casa ha quedado rodeada por una inundación y está aislada.'}, {optionId: 'D', text: 'Está intentando rescatar los muebles de sus vecinos.'}] },
            { questionId: 'q_2025_w01_read_01_02', textId: 'txt_2025_w01_read_01', packId: packId, prompt: '¿Qué le ocurrió a la casa de la mujer durante la noche?', correctOptionId: 'C', difficulty: 2, explanationText: 'La casa se movió violentamente por el agua, pero el texto dice "hasta donde dio la amarra", indicando que se balanceó pero se mantuvo sujeta.', options: [{optionId: 'A', text: 'Se soltó de sus amarras y flotó río abajo.'}, {optionId: 'B', text: 'Fue golpeada por un árbol y se rompió una ventana.'}, {optionId: 'C', text: 'Se balanceó por la corriente pero se mantuvo amarrada.'}, {optionId: 'D', text: 'Se inundó completamente y tuvo que subir al techo.'}] },
            { questionId: 'q_2025_w01_read_01_03', textId: 'txt_2025_w01_read_01', packId: packId, prompt: 'En el texto, ¿a qué se refiere la palabra "regalo"?', correctOptionId: 'A', difficulty: 2, explanationText: 'La mujer estaba "muerta de hambre". Al llegar el felino, ella lo ve como una oportunidad de alimento para sobrevivir.', options: [{optionId: 'A', text: 'A la oportunidad de cazar a la pantera para alimentarse.'}, {optionId: 'B', text: 'A la llegada inesperada de un equipo de rescate.'}, {optionId: 'C', text: 'A la madera que trajo el árbol.'}, {optionId: 'D', text: 'Al rifle que encontró escondido bajo la cama.'}] },
            { questionId: 'q_2025_w01_read_01_04', textId: 'txt_2025_w01_read_01', packId: packId, prompt: '¿Qué pista nos da el texto para saber que la mujer está preparada para defenderse?', correctOptionId: 'B', difficulty: 1, explanationText: 'La frase "El rifle se movió sobre sus rodillas" indica que tiene un arma lista para usar.', options: [{optionId: 'A', text: 'Menciona que ha puesto trampas alrededor de la casa.'}, {optionId: 'B', text: 'Dice que el rifle se movió sobre sus rodillas.'}, {optionId: 'C', text: 'Ella grita fuertemente para asustar al animal.'}, {optionId: 'D', text: 'Describe cómo bloqueó todas las puertas.'}] },
            { questionId: 'q_2025_w01_read_02_01', textId: 'txt_2025_w01_read_02', packId: packId, prompt: '¿Cuál es el propósito principal de la carta de Helga?', correctOptionId: 'B', difficulty: 2, explanationText: 'Helga expresa "rabia" y se queja de los costos sociales de limpiar los grafitis.', options: [{optionId: 'A', text: 'Explicar cómo se limpian las paredes pintadas.'}, {optionId: 'B', text: 'Expresar su enojo por los costos que generan los grafitis.'}, {optionId: 'C', text: 'Promover una exposición de arte para jóvenes.'}, {optionId: 'D', text: 'Felicitar a los jóvenes por su creatividad artística.'}] },
            { questionId: 'q_2025_w01_read_02_02', textId: 'txt_2025_w01_read_02', packId: packId, prompt: '¿Qué argumento utiliza Sofía para defender los grafitis?', correctOptionId: 'B', difficulty: 3, explanationText: 'Sofía compara los grafitis con la publicidad, argumentando que nadie pide permiso para poner vallas publicitarias gigantes.', options: [{optionId: 'A', text: 'Que los grafitis son mucho más baratos que la publicidad.'}, {optionId: 'B', text: 'Que la publicidad tampoco pide permiso para ser vista.'}, {optionId: 'C', text: 'Que los grafitis aumentan el valor de las propiedades.'}, {optionId: 'D', text: 'Que los grafiteros son artistas profesionales contratados.'}] },
            { questionId: 'q_2025_w01_read_02_03', textId: 'txt_2025_w01_read_02', packId: packId, prompt: '¿Por qué menciona Sofía la publicidad y la ropa en su carta?', correctOptionId: 'A', difficulty: 3, explanationText: 'Para mostrar la contradicción de la sociedad que acepta el estilo graffiti en la moda pero lo rechaza en las paredes.', options: [{optionId: 'A', text: 'Para demostrar que el estilo de los grafitis ha influido en la moda aceptada.'}, {optionId: 'B', text: 'Para quejarse de que la ropa es demasiado cara.'}, {optionId: 'C', text: 'Para sugerir que los grafiteros deberían diseñar ropa.'}, {optionId: 'D', text: 'Para criticar la mala calidad de las vallas publicitarias.'}] },
            { questionId: 'q_2025_w01_read_02_04', textId: 'txt_2025_w01_read_02', packId: packId, prompt: '¿En qué punto están de acuerdo, implícita o explícitamente, ambas autoras?', correctOptionId: 'B', difficulty: 2, explanationText: 'Ambas reconocen que es una forma de expresión: Helga menciona la "creatividad" y Sofía la "comunicación".', options: [{optionId: 'A', text: 'En que los grafitis son una forma de vandalismo criminal.'}, {optionId: 'B', text: 'En que los grafitis son una forma de expresión o comunicación visual.'}, {optionId: 'C', text: 'En que la limpieza de las paredes es muy costosa.'}, {optionId: 'D', text: 'En que no se debe permitir ninguna publicidad en las calles.'}] },
            { questionId: 'q_2025_w01_read_03_01', textId: 'txt_2025_w01_read_03', packId: packId, prompt: 'Según el texto, ¿cuándo desapareció el Lago Chad por completo?', correctOptionId: 'C', difficulty: 1, explanationText: 'El texto dice explícitamente: "Año 20.000 a.C.: El lago desapareció por completo".', options: [{optionId: 'A', text: 'Alrededor del año 11.000 a.C.'}, {optionId: 'B', text: 'Alrededor del año 4.000 a.C.'}, {optionId: 'C', text: 'Alrededor del año 20.000 a.C.'}, {optionId: 'D', text: 'En la actualidad.'}] },
            { questionId: 'q_2025_w01_read_03_02', textId: 'txt_2025_w01_read_03', packId: packId, prompt: '¿Cuál es la situación actual del nivel del lago comparada con el pasado?', correctOptionId: 'B', difficulty: 1, explanationText: 'El texto indica que actualmente es aproximadamente el mismo nivel que tenía en el año 1.000 d.C.', options: [{optionId: 'A', text: 'Es más alto que nunca en la historia.'}, {optionId: 'B', text: 'Es similar al nivel que tenía en el año 1.000 d.C.'}, {optionId: 'C', text: 'El lago ha desaparecido nuevamente por completo.'}, {optionId: 'D', text: 'Es mucho más profundo que en el año 4.000 a.C.'}] },
            { questionId: 'q_2025_w01_read_03_03', textId: 'txt_2025_w01_read_03', packId: packId, prompt: 'Si encontramos arte rupestre con dibujos de hipopótamos en la zona, ¿qué podemos inferir?', correctOptionId: 'B', difficulty: 2, explanationText: 'El arte rupestre refleja lo que los artistas veían. Si dibujaron hipopótamos, es porque vivían allí en esa época húmeda.', options: [{optionId: 'A', text: 'Que los artistas viajaron a zoológicos lejanos.'}, {optionId: 'B', text: 'Que los hipopótamos existían en la zona cuando se hicieron los dibujos.'}, {optionId: 'C', text: 'Que los hipopótamos fueron traídos por humanos recientemente.'}, {optionId: 'D', text: 'Que el lago estaba seco cuando se dibujaron.'}] },
            { questionId: 'q_2025_w01_read_03_04', textId: 'txt_2025_w01_read_03', packId: packId, prompt: '¿Qué relación temporal existe entre la reaparición del lago y su nivel máximo?', correctOptionId: 'B', difficulty: 2, explanationText: 'Reapareció en 11.000 a.C. y alcanzó máximo en 4.000 a.C. La diferencia es 7.000 años.', options: [{optionId: 'A', text: 'El lago alcanzó su máximo nivel inmediatamente después.'}, {optionId: 'B', text: 'Pasaron unos 7.000 años entre su reaparición y su nivel máximo.'}, {optionId: 'C', text: 'El lago reapareció después de alcanzar su nivel máximo.'}, {optionId: 'D', text: 'El lago nunca reapareció después de la era glacial.'}] },
            // === CIENCIAS ===
            { questionId: 'q_2025_w01_sci_01', textId: 'txt_2025_w01_sci_01', packId: packId, prompt: '¿Cuál es el riesgo biológico más probable de no completar el ciclo de antibióticos recetado?', correctOptionId: 'B', difficulty: 2, explanationText: 'Interrumpir el tratamiento permite que las bacterias más resistentes sobrevivan y se reproduzcan, generando resistencia futura.', options: [{optionId: 'A', text: 'El sistema inmunológico se vuelve adicto al medicamento.'}, {optionId: 'B', text: 'Las bacterias sobrevivientes pueden desarrollar resistencia al antibiótico.'}, {optionId: 'C', text: 'Los glóbulos blancos dejan de producirse.'}, {optionId: 'D', text: 'El hígado se intoxica.'}] },
            { questionId: 'q_2025_w01_sci_02', textId: 'txt_2025_w01_sci_02', packId: packId, prompt: '¿Qué gas contienen principalmente esas burbujas y por qué proceso se producen?', correctOptionId: 'C', difficulty: 2, explanationText: 'En la fotosíntesis, las plantas usan luz para convertir agua y CO2 en glucosa, liberando oxígeno como subproducto.', options: [{optionId: 'A', text: 'Dióxido de carbono producido por la respiración.'}, {optionId: 'B', text: 'Nitrógeno absorbido del agua.'}, {optionId: 'C', text: 'Oxígeno producido por la fotosíntesis.'}, {optionId: 'D', text: 'Vapor de agua por el calor de la lámpara.'}] },
            { questionId: 'q_2025_w01_sci_03', textId: 'txt_2025_w01_sci_03', packId: packId, prompt: '¿Cuál es la función principal de introducir estos fragmentos en una persona sana?', correctOptionId: 'B', difficulty: 2, explanationText: 'Las vacunas entrenan al sistema inmune para reconocer el patógeno sin causar la enfermedad.', options: [{optionId: 'A', text: 'Matar a los virus reales que ya están en el aire.'}, {optionId: 'B', text: 'Enseñar al sistema inmune a reconocer y atacar al patógeno en el futuro.'}, {optionId: 'C', text: 'Fortalecer los huesos para evitar la entrada de enfermedades.'}, {optionId: 'D', text: 'Curar una enfermedad que el paciente ya padece.'}] },
            { questionId: 'q_2025_w01_sci_04', textId: 'txt_2025_w01_sci_04', packId: packId, prompt: '¿Qué efecto inmediato se esperaría en la cadena trófica del estanque?', correctOptionId: 'A', difficulty: 3, explanationText: 'Sin ranas, los mosquitos (presa) aumentan por falta de depredador, y las serpientes (depredador) disminuyen por falta de comida.', options: [{optionId: 'A', text: 'Aumento de la población de mosquitos y disminución de serpientes.'}, {optionId: 'B', text: 'Disminución de la población de mosquitos.'}, {optionId: 'C', text: 'Aumento de la población de plantas acuáticas.'}, {optionId: 'D', text: 'Aumento de la población de serpientes.'}] },
            { questionId: 'q_2025_w01_sci_05', textId: 'txt_2025_w01_sci_05', packId: packId, prompt: '¿Por qué el cuerpo responde de esta manera durante el esfuerzo físico intenso?', correctOptionId: 'B', difficulty: 1, explanationText: 'Los músculos necesitan más oxígeno y nutrientes para producir energía, lo que requiere que el corazón bombee más rápido.', options: [{optionId: 'A', text: 'Para eliminar el exceso de agua a través de los pulmones.'}, {optionId: 'B', text: 'Para suministrar más oxígeno y nutrientes a los músculos activos.'}, {optionId: 'C', text: 'Para reducir la temperatura de la sangre rápidamente.'}, {optionId: 'D', text: 'Para disminuir la presión arterial en las extremidades.'}] },
            { questionId: 'q_2025_w01_sci_06', textId: 'txt_2025_w01_sci_06', packId: packId, prompt: '¿A qué se debe este cambio de sabor en el pan?', correctOptionId: 'B', difficulty: 2, explanationText: 'La enzima amilasa rompe el almidón (que no es dulce) en azúcares simples como glucosa (que sí es dulce).', options: [{optionId: 'A', text: 'El pan absorbe los azúcares naturales de la lengua.'}, {optionId: 'B', text: 'La amilasa descompone el almidón en azúcares más simples.'}, {optionId: 'C', text: 'El agua de la saliva disuelve la corteza del pan.'}, {optionId: 'D', text: 'La masticación crea calor que carameliza el pan.'}] },
            { questionId: 'q_2025_w01_sci_07', textId: 'txt_2025_w01_sci_07', packId: packId, prompt: 'Según la teoría de Darwin, ¿qué sucederá con la población de mariposas?', correctOptionId: 'B', difficulty: 2, explanationText: 'La selección natural favorece a los que mejor se camuflan. En árboles negros, las oscuras sobreviven mejor a los depredadores.', options: [{optionId: 'A', text: 'Las blancas cambiarán su color voluntariamente.'}, {optionId: 'B', text: 'Las mariposas oscuras tendrán más éxito reproductivo por estar mejor camufladas.'}, {optionId: 'C', text: 'Ambas poblaciones morirán por la contaminación.'}, {optionId: 'D', text: 'Las blancas se mudarán a otro bosque.'}] },
            { questionId: 'q_2025_w01_sci_08', textId: 'txt_2025_w01_sci_08', packId: packId, prompt: '¿Qué le sucede a las células de una persona con diabetes no tratada?', correctOptionId: 'B', difficulty: 2, explanationText: 'La insulina permite que la glucosa entre a la célula. Sin insulina, la glucosa se queda afuera y la célula no obtiene energía.', options: [{optionId: 'A', text: 'Tienen demasiada energía por el exceso de azúcar.'}, {optionId: 'B', text: 'No pueden absorber la glucosa de la sangre para obtener energía.'}, {optionId: 'C', text: 'Se multiplican más rápido de lo normal.'}, {optionId: 'D', text: 'Absorben demasiada agua y explotan.'}] },
            { questionId: 'q_2025_w01_sci_09', textId: 'txt_2025_w01_sci_09', packId: packId, prompt: '¿Cuál es la probabilidad de que su primer hijo tenga el lóbulo de la oreja pegado?', correctOptionId: 'C', difficulty: 3, explanationText: 'Cruce Dd x dd. Resultados posibles: Dd, Dd, dd, dd. El 50% es dd (pegado).', options: [{optionId: 'A', text: '0%'}, {optionId: 'B', text: '25%'}, {optionId: 'C', text: '50%'}, {optionId: 'D', text: '100%'}] },
            { questionId: 'q_2025_w01_sci_10', textId: 'txt_2025_w01_sci_10', packId: packId, prompt: '¿Cuál es el mecanismo físico-biológico por el cual el sudor enfría el cuerpo?', correctOptionId: 'B', difficulty: 2, explanationText: 'La evaporación absorbe calor. El sudor toma el calor de la piel para convertirse en gas, enfriando el cuerpo.', options: [{optionId: 'A', text: 'El sudor es líquido frío que sale del interior.'}, {optionId: 'B', text: 'Al evaporarse de la piel, el sudor absorbe energía térmica del cuerpo.'}, {optionId: 'C', text: 'El sudor bloquea los rayos del sol.'}, {optionId: 'D', text: 'El sudor aumenta la superficie de contacto con el aire frío.'}] },
            // === MATEMÁTICAS ===
            { questionId: 'q_2025_w01_math_01', textId: 'txt_2025_w01_math_01', packId: packId, prompt: 'Si se aplica la fórmula a la manera de caminar de Enrique (70 pasos/min), ¿cuál es su longitud de paso?', correctOptionId: 'C', difficulty: 2, explanationText: 'n/p = 140 -> 70/p = 140 -> p = 70/140 = 0.5 metros.', options: [{optionId: 'A', text: '2.0 metros'}, {optionId: 'B', text: '0.2 metros'}, {optionId: 'C', text: '0.5 metros'}, {optionId: 'D', text: '7.0 metros'}] },
            { questionId: 'q_2025_w01_math_02', textId: 'txt_2025_w01_math_02', packId: packId, prompt: 'Si cambia 3 000 dólares de Singapur, ¿cuánto dinero recibió Mei-Ling en rands sudafricanos?', correctOptionId: 'A', difficulty: 1, explanationText: 'Multiplicamos la cantidad por el tipo de cambio: 3000 x 4.2 = 12 600 ZAR.', options: [{optionId: 'A', text: '12 600 ZAR'}, {optionId: 'B', text: '714 ZAR'}, {optionId: 'C', text: '12 000 ZAR'}, {optionId: 'D', text: '4 200 ZAR'}] },
            { questionId: 'q_2025_w01_math_03', textId: 'txt_2025_w01_math_03', packId: packId, prompt: '¿Cuál es la media de las notas de Irene en ciencias tras los cinco exámenes?', correctOptionId: 'B', difficulty: 2, explanationText: 'Total inicial = 60 * 4 = 240. Nuevo total = 240 + 80 = 320. Nueva media = 320 / 5 = 64.', options: [{optionId: 'A', text: '70'}, {optionId: 'B', text: '64'}, {optionId: 'C', text: '60'}, {optionId: 'D', text: '75'}] },
            { questionId: 'q_2025_w01_math_04', textId: 'txt_2025_w01_math_04', packId: packId, prompt: '¿Cuántas repisas completas puede construir este carpintero con el material disponible?', correctOptionId: 'B', difficulty: 3, explanationText: 'Limitante: Tablas cortas (33/6 = 5.5). Solo alcanza para 5 repisas completas antes de que se acabe ese material.', options: [{optionId: 'A', text: '6'}, {optionId: 'B', text: '5'}, {optionId: 'C', text: '10'}, {optionId: 'D', text: '33'}] },
            { questionId: 'q_2025_w01_math_05', textId: 'txt_2025_w01_math_05', packId: packId, prompt: '¿Cuántas combinaciones diferentes de dos ingredientes extra podría seleccionar Jaime?', correctOptionId: 'B', difficulty: 2, explanationText: 'Combinaciones de 4 elementos tomados de 2 en 2: (4*3)/2 = 6 pares posibles.', options: [{optionId: 'A', text: '4'}, {optionId: 'B', text: '6'}, {optionId: 'C', text: '8'}, {optionId: 'D', text: '12'}] },
            { questionId: 'q_2025_w01_math_06', textId: 'txt_2025_w01_math_06', packId: packId, prompt: 'Calcula aproximadamente la distancia total recorrida por la Mir (redondea a decenas de millón).', correctOptionId: 'A', difficulty: 3, explanationText: 'Diámetro órbita = 12700 + 800 = 13500. Circunferencia = 13500*PI ≈ 42412. Total = 42412 * 86500 ≈ 3668 millones -> 3670 millones.', options: [{optionId: 'A', text: '3 670 millones de km'}, {optionId: 'B', text: '3 460 millones de km'}, {optionId: 'C', text: '3 800 millones de km'}, {optionId: 'D', text: '4 240 millones de km'}] },
            { questionId: 'q_2025_w01_math_07', textId: 'txt_2025_w01_math_07', packId: packId, prompt: '¿Cuántos cuadrados en total deberá usar para construir la Etapa 4 del esquema?', correctOptionId: 'C', difficulty: 2, explanationText: 'Etapa 1=1, Etapa 2=3 (+2), Etapa 3=6 (+3). Etapa 4 será 6 + 4 = 10 cuadrados.', options: [{optionId: 'A', text: '9 cuadrados'}, {optionId: 'B', text: '12 cuadrados'}, {optionId: 'C', text: '10 cuadrados'}, {optionId: 'D', text: '8 cuadrados'}] },
            { questionId: 'q_2025_w01_math_08', textId: 'txt_2025_w01_math_08', packId: packId, prompt: 'Encuentra el valor para n donde el número de manzanos es igual al de pinos.', correctOptionId: 'B', difficulty: 2, explanationText: 'Igualamos fórmulas: n*n = 8*n. Dividimos por n: n = 8.', options: [{optionId: 'A', text: 'n = 4'}, {optionId: 'B', text: 'n = 8'}, {optionId: 'C', text: 'n = 16'}, {optionId: 'D', text: 'n = 2'}] },
            { questionId: 'q_2025_w01_math_09', textId: 'txt_2025_w01_math_09', packId: packId, prompt: '¿Qué pizza es la mejor opción en relación a lo que cuesta?', correctOptionId: 'B', difficulty: 3, explanationText: 'La pizza grande cuesta menos por cm² (0.10 zeds) comparada con la pequeña (0.133 zeds).', options: [{optionId: 'A', text: 'La pizza pequeña.'}, {optionId: 'B', text: 'La pizza grande.'}, {optionId: 'C', text: 'Ambas son igual de rentables.'}, {optionId: 'D', text: 'No se puede determinar.'}] },
            { questionId: 'q_2025_w01_math_10', textId: 'txt_2025_w01_math_10', packId: packId, prompt: '¿A partir de qué edad aumenta la máxima frecuencia cardíaca recomendada con la nueva fórmula?', correctOptionId: 'B', difficulty: 3, explanationText: 'Igualando 220-edad = 208-0.7*edad, se cruzan a los 40 años. A partir de los 41 la nueva fórmula da valores más altos.', options: [{optionId: 'A', text: '20 años'}, {optionId: 'B', text: '41 años'}, {optionId: 'C', text: '60 años'}, {optionId: 'D', text: '40 años'}] }
        ];

        // Loop de inserción con Tags inteligentes
        for (const question of questions) {
            const questionRef = db.collection('questions').doc(question.questionId);
            await questionRef.set({
                questionId: question.questionId,
                textId: question.textId,
                packId: question.packId,
                prompt: question.prompt,
                correctOptionId: question.correctOptionId,
                difficulty: question.difficulty,
                explanationText: question.explanationText,
                explanationStatus: 'APPROVED',
                options: question.options,
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
                updatedAt: admin.firestore.FieldValue.serverTimestamp(),
                status: 'PUBLISHED',
                // Lógica de tags dinámica
                tags: question.questionId.includes('read') ? ['comprension', 'inferencia'] :
                      question.questionId.includes('math') ? ['matematica', 'logica'] :
                      ['ciencias', 'biologia']
            });
            console.log(`   ✅ Pregunta creada: ${question.questionId}`);
        }

        // 4. Actualizar Pack
        await packRef.update({
            textIds: texts.map(t => t.textId),
            questionIds: questions.map(q => q.questionId),
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
        });

        console.log('\n✨ Pack Semana 01 (Real) finalizado con éxito.');

    } catch (error) {
        console.error('❌ Error cargando datos:', error);
        throw error;
    }
}

// EJECUTAR
initFirestore()
    .then(() => {
        console.log('🏁 Proceso terminado. Cierra con Ctrl+C si no termina solo.');
        process.exit(0);
    })
    .catch((error) => {
        console.error('❌ Falló la ejecución.');
        process.exit(1);
    });