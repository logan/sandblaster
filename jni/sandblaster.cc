#include <jni.h>
#include <stdlib.h>
#include <sys/endian.h>
#include <time.h>

#include <android/log.h>

#define LOG(...) __android_log_print(ANDROID_LOG_INFO, "com.loganh.sandblaster", __VA_ARGS__)

extern JavaVM* jvm;

static int NEIGHBORS[][2] = {
  { 0, 1 },
  { 1, 1 },
  { 1, 0 },
  { 1, -1 },
  { 0, -1 },
  { -1, -1 },
  { -1, 0 },
  { -1, 1 },
};

struct DataStream {
  JNIEnv* env;
  jbyteArray byte_array;
  char* data;
  jint pos;
  jint size;

  DataStream(JNIEnv* env, jbyteArray byte_array) {
    this->env = env;
    this->byte_array = byte_array;
    pos = 0;
    size = env->GetArrayLength(byte_array);
    this->data = (char*) env->GetPrimitiveArrayCritical(byte_array, NULL);
  }

  ~DataStream() {
    this->env->ReleasePrimitiveArrayCritical(byte_array, data, NULL);
  }

  void Skip(jint n) {
    pos += n;
  }

  jbyte ReadJbyte() {
    if (pos + 1 > size) {
      // TODO: exception
      return 0;
    }
    return data[pos++];
  }

  jboolean ReadJboolean() {
    return (jboolean) ReadJbyte();
  }

  jshort ReadJshort() {
    if (pos + 2 > size) {
      // TODO: exception
      return 0;
    }
    jchar result = betoh16(*(jchar*) (data + pos));
    pos += 2;
    return result;
  }

  jint ReadJint() {
    if (pos + 4 > size) {
      // TODO: exception
      LOG("EOF on ReadJint");
      return 0;
    }
    jint result = betoh32(*(jint*) (data + pos));
    pos += 4;
    return result;
  }

  jfloat ReadJfloat() {
    jint i = ReadJint();
    return *(jfloat *) &i;
  }
};

struct RNG {
  static jboolean rng_initialized;

  RNG() {
    if (!rng_initialized) {
      time_t now;
      time(&now);
      srandom(now);
      rng_initialized = 1;
    }
  }

  jboolean NextBoolean() {
    return random() & 1;
  }

  jfloat NextFloat() {
    jfloat result = random() / (float(0x7fffffff) + 1);
    return result;
  }
};


jboolean RNG::rng_initialized = 0;


template <class T>
struct List {
  struct ListNode {
    T* data;
    ListNode* next;
  };
  typedef ListNode* Node;
  Node head;

  void add(T* data) {
    Node node = new ListNode;
    node->data = data;
    node->next = head;
    head = node;
  }

  bool remove(T* data) {
    Node prev = 0;
    for (Node n = head; n; prev = n, n = n->next) {
      if (n->data == data) {
        if (prev) {
          prev->next = n->next;
        } else {
          head = n->next;
        }
        delete n;
        return true;
      }
    }
    return false;
  }
};


template <class T>
struct Grid {
  struct GridCol {
    T null_value;
    T* data;
    jint h;

    ~GridCol() {
      if (data) {
        delete[] data;
      }
    }

    void Init(jint h) {
      this->h = h;
      data = new T[h];
    }

    T& operator[](jint y) {
      if (y >= 0 && y < h) {
        return data[y];
      } else {
        return null_value = T();
      }
    }
  };

  GridCol null_col;
  GridCol* data;
  jint w;

  Grid(jint w, jint h) {
    this->w = w;
    data = new GridCol[w];
    for (jint x = 0; x < w; x++) {
      data[x].Init(h);
    }
  }

  ~Grid() {
    delete[] data;
  }

  GridCol& operator[](jint x) {
    if (x >= 0 && x < w) {
      return data[x];
    }
    return null_col;
  }
};


struct Element;
struct ElementTable;

struct ProductSet {
  jint size;
  Element** products;
  jfloat* weights;
  jfloat total_weight;

  ProductSet() : size(0) {}

  ~ProductSet();

  ProductSet& operator=(const ProductSet& ps);
  void Read(DataStream* stream, ElementTable* table);
  Element* PickProduct();
};


struct Transmutation {
  jfloat probability;
  Element* target;
  ProductSet products;

  Transmutation() : probability(0), target(NULL) {}

  Transmutation& operator=(const Transmutation& t) {
    probability = t.probability;
    target = t.target;
    products = t.products;
    return *this;
  }

  void Read(DataStream* stream, ElementTable* table);
};


struct Element {
  jbyte ordinal;
  jshort id;
  jint color;
  jboolean mobile;
  jfloat density;
  jfloat viscosity;
  jint lifetime;
  jint transmutation_count;
  jfloat decay_probability;
  ProductSet decay_products;

  void Read(DataStream* stream) {
    // Ignore the name.
    jshort n = stream->ReadJshort();
    stream->Skip(n);

    id = stream->ReadJshort();
    color = stream->ReadJint();

    // Skip drawable
    stream->ReadJboolean();

    mobile = stream->ReadJboolean();
    density = stream->ReadJfloat();
    viscosity = stream->ReadJfloat();
    decay_probability = stream->ReadJfloat();
    lifetime = stream->ReadJint();
  }
};


struct ElementTable {
  jint size;
  Element* elements;
  Transmutation* transmutations;

  ~ElementTable() {
    if (size) {
      delete[] elements;
      delete[] transmutations;
    }
  }

  Element* GetElementById(jchar id) {
    for (jint i = 0; i < size; i++) {
      if (elements[i].id == id) {
        return elements + i;
      }
    }
    return NULL;
  }

  Element* GetElementByOrdinal(int ord) {
    return (ord >= 0 && ord < size) ? elements + ord : NULL;
  }

  void Read(DataStream* stream) {
    size = stream->ReadJbyte();
    if (elements) { delete[] elements; }
    if (transmutations) { delete[] transmutations; }
    elements = new Element[size];
    transmutations = new Transmutation[size * size];
    for (jint i = 0; i < size; i++) {
      Element* elem = elements + i;
      elem->ordinal = i;
      elem->transmutation_count = 0;
      elem->Read(stream);
    }

    // Decay products
    for (jint i = 0; i < size; i++) {
      elements[i].decay_products.Read(stream, this);
    }

    // Transmutations
    jbyte ord = stream->ReadJbyte();
    Element* agent = GetElementByOrdinal(ord);
    while (agent) {
      agent->transmutation_count++;
      Transmutation t;
      t.Read(stream, this);
      transmutations[ord * size + t.target->ordinal] = t;
      ord = stream->ReadJbyte();
      agent = GetElementByOrdinal(ord);
    }
  }

  Element* MaybeTransmutate(Element* agent, Element* target) {
    if (!agent || !target) {
      return target;
    }
    Transmutation& t = transmutations[agent->ordinal * size + target->ordinal];
    if (t.probability <= 0) {
      return target;
    }
    RNG rng;
    if (rng.NextFloat() < t.probability) {
      return t.products.PickProduct();
    }
    return target;
  }
};


ProductSet& ProductSet::operator=(const ProductSet& ps)  {
  size = ps.size;
  products = new Element*[size];
  weights = new jfloat[size];
  for (int i = 0; i < size; i++) {
    products[i] = ps.products[i];
    weights[i] = ps.weights[i];
  }
  total_weight = ps.total_weight;
  return *this;
}

ProductSet::~ProductSet() {
  if (size) {
    delete[] products;
    delete[] weights;
  }
}

void ProductSet::Read(DataStream* stream, ElementTable* table) {
  if (size) {
    delete[] products;
    delete[] weights;
  }
  size = stream->ReadJbyte();
  products = new Element*[size];
  weights = new jfloat[size];
  total_weight = 0;
  for (int i = 0; i < size; i++) {
    jbyte ord = stream->ReadJbyte();
    products[i] = table->GetElementByOrdinal(ord);
  }
  for (int i = 0; i < size; i++) {
    weights[i] = stream->ReadJfloat();
    total_weight += weights[i];
  }
}

Element* ProductSet::PickProduct() {
  if (!size) {
    return NULL;
  }
  if (size == 0) {
    return products[0];
  }
  RNG rng;
  jfloat w = rng.NextFloat() * total_weight;
  for (int i = 0; i < size; i++) {
    if (w <= weights[i]) {
      return products[i];
    }
    w -= weights[i];
  }
  return products[0];
}

void Transmutation::Read(DataStream* stream, ElementTable* table) {
  jbyte ord = stream->ReadJbyte();
  target = table->GetElementByOrdinal(ord);
  probability = stream->ReadJfloat();
  products.Read(stream, table);
}


struct Point {
  Element* element;
  jint age;
  jint last_set;
  jint last_changed;
  jint last_floated;

  Point& operator=(Element* element) {
    if (element != this->element) {
      age = 0;
    }
    this->element = element;
    return *this;
  }
};


struct Sandbox {

  typedef List<Sandbox> SandboxList;
  static SandboxList sandboxes;

  jobject java_ref;
  jint w;
  jint h;
  jint iteration;
  RNG rng;
  ElementTable* elements;

  // 2D map of element pointers. NULL means no particle.
  Grid<Point> points;

  // 1D array representation of the resulting bitmap.
  jintArray pixels;
  jintArray pixelsGlobalRef;
  

  Sandbox(JNIEnv* env, jint w, jint h, jobject java_ref) : points(w, h) {
    this->w = w;
    this->h = h;
    this->java_ref = java_ref;
    elements = new ElementTable();
    jintArray localPixelsRef = (jintArray) env->NewIntArray(w * h);
    pixels = (jintArray) env->NewGlobalRef(localPixelsRef);
    env->DeleteLocalRef(localPixelsRef);
  }

  ~Sandbox() {
    delete elements;
    JNIEnv *env;
    jvm->AttachCurrentThread(&env, NULL);
    env->DeleteGlobalRef(pixels);
    env->DeleteGlobalRef(java_ref);
  }

  void Read(DataStream* stream) {
    LOG("reading version");
    jfloat version = stream->ReadJfloat();
    LOG("serialization version: %f", version);
    if (version != 1.6f) {
      // TODO: throw exception
      LOG("  can't parse this version");
      return;
    }
    delete elements;
    elements = new ElementTable();
    elements->Read(stream);
  }

  void Resize(jint w, jint h) {
    JNIEnv *env;
    jvm->AttachCurrentThread(&env, NULL);
    env->DeleteGlobalRef(pixels);
    this->w = w;
    this->h = h;
    jintArray localPixelsRef = (jintArray) env->NewIntArray(w * h);
    pixels = (jintArray) env->NewGlobalRef(localPixelsRef);
    env->DeleteLocalRef(localPixelsRef);
  }

  static SandboxList::Node Lookup(JNIEnv* env, jobject thiz) {
    for (SandboxList::Node n = sandboxes.head; n; n = n->next) {
      if (env->IsSameObject(thiz, n->data->java_ref)) {
        return n;
      }
    }
    return NULL;
  }

  static Sandbox* Get(JNIEnv* env, jobject thiz) {
    SandboxList::Node n = Lookup(env, thiz);
    if (n) {
      return n->data;
    } else {
      return Create(env, thiz);
    }
  }

  static Sandbox* Create(JNIEnv* env, jobject thiz) {
    static jfieldID widthFid, heightFid;
    if (!widthFid) {
      jclass cls = env->GetObjectClass(thiz);
      widthFid = env->GetFieldID(cls, "width", "I");
      heightFid = env->GetFieldID(cls, "height", "I");
    }
    jint w = env->GetIntField(thiz, widthFid);
    jint h = env->GetIntField(thiz, heightFid);
    Sandbox* sandbox = new Sandbox(env, w, h, env->NewGlobalRef(thiz));
    sandboxes.add(sandbox);
    return sandbox;
  }

  static void Delete(JNIEnv* env, jobject thiz) {
    SandboxList::Node n = Lookup(env, thiz);
    sandboxes.remove(n->data);
  }

  void Clear() {
    for (jint y = 0; y < h; y++) {
      for (jint x = 0; x < w; x++) {
        points[x][y] = NULL;
      }
    }
  }

  Element* GetElement(JNIEnv* env, jobject jelement) {
    if (!jelement) {
      return NULL;
    }
    static jfieldID idFid;
    if (!idFid) {
      jclass cls = env->GetObjectClass(jelement);
      idFid = env->GetFieldID(cls, "id", "C");
    }
    jchar id = env->GetCharField(jelement, idFid);
    return elements->GetElementById(id);
  }

  void SetParticle(jint x, jint y, Element* elem) {
    if (elem != points[x][y].element) {
      points[x][y].last_changed = iteration;
    }
    points[x][y].last_set = iteration;
    points[x][y] = elem;
  }

  void SetParticle(jint x, jint y, Element* elem, jint radius, jfloat prob) {
    jint r2 = radius * radius;
    for (jint i = -radius; i < radius; i++) {
      for (jint j = -radius; j < radius; j++) {
        if (i * i + j * j <= r2
            && (!elem || !elem->mobile || rng.NextFloat() < prob)) {
          SetParticle(x + i, y + j, elem);
        }
      }
    }
  }

  void Line(Element* element, jint radius, jint x1, jint y1, jint x2, jint y2) {
    jint dx = x1 - x2;
    jint dy = y1 - y2;
    jint d = (abs(dx) > abs(dy)) ? abs(dx) : abs(dy);
    for (int i = 0; i <= d; i++) {
      int x = x2 + ((jfloat) i / d) * dx + .5;
      int y = y2 + ((jfloat) i / d) * dy + .5;
      SetParticle(x, y, element, radius, 0.1f);
    }
  }

  void Line(Element* element, jint x1, jint y1, jint x2, jint y2) {
    Line(element, 0, x1, y1, x2, y2);
  }

  void Swap(jint x1, jint y1, jint x2, jint y2) {
    if (x1 < 0 || y1 < 0 || x1 >= w || y2 >= h) {
      SetParticle(x2, y2, NULL);
    } else if (x2 < 0 || y2 < 0 || x2 >= w || y2 >= h) {
      SetParticle(x1, y1, NULL);
    } else {
      Point& p1 = points[x1][y1];
      Point& p2 = points[x2][y2];
      Element* elem = p1.element;
      jint left_age = p1.age;
      jint right_age = p2.age;
      SetParticle(x1, y1, p2.element);
      SetParticle(x2, y2, elem);
      p1.age = right_age;
      p2.age = left_age;
    }
  }

  void Iterate() {
    // TODO: sources

    ++iteration;

    for (jint y = 0; y < h; y++) {
      jint start = 0;
      jint last = w;
      jint dir = 1;
      if (rng.NextBoolean()) {
        start = w - 1;
        last = -1;
        dir = -1;
      }
      for (jint x = start; x != last; x += dir) {
        Element* e = points[x][y].element;
        if (!e) {
          continue;
        }

        // Vertical movement.
        if (y == 0 && e->density > 0) {
          // Drop off the screen.
          SetParticle(x, y, NULL);
          continue;
        }
        if (y == h - 1 && e->density < 0) {
          // Float off the screen.
          SetParticle(x, y, NULL);
        }

        jint cur_last_set = points[x][y].last_set;

        // Transmutations.
        if (e->transmutation_count > 0 && cur_last_set != iteration) {
          for (int i = 0; i < sizeof(NEIGHBORS) / sizeof(NEIGHBORS[0]); i++) {
            int nx = x + NEIGHBORS[i][0];
            int ny = y + NEIGHBORS[i][1];
            if (nx >= 0 && nx < w && ny >= 0 & ny < h && points[nx][ny].last_set != iteration) {
              Element* t = points[nx][ny].element;
              if (t) {
                Element* o = elements->MaybeTransmutate(e, t);
                if (o != points[nx][ny].element) {
                  SetParticle(nx, ny, o);
                }
              }
            }
          }
        }

        // Decay.
        if (cur_last_set != iteration && e->decay_probability > 0 && rng.NextFloat() < e->decay_probability) {
          points[x][y].age++;
          if (points[x][y].age > e->lifetime) {
            SetParticle(x, y, e->decay_products.PickProduct());
            continue;
          }
        }

        if (!e->mobile || cur_last_set == iteration) {
          continue;
        }

        // Horizontal movement.
        if (rng.NextFloat() < e->viscosity) {
          jint nx = x + (rng.NextBoolean() ? 1 : -1); 
          if (e->density > 0) {
            // Slide only if blocked below.
            if (y - 1 >= 0) {
              Element* o = points[x][y - 1].element;
              if (o && (!o->mobile || e->density <= o->density)) {
                Element* p = (nx < 0 || nx >= w) ? NULL : points[nx][y].element;
                if (!p || (p->mobile && e->density > p->density && rng.NextFloat() < e->density - p->density)) {
                  if (nx < 0 || nx >= w || points[nx][y - 1].last_floated != iteration) {
                    Swap(x, y, nx, y);
                    points[x][y].last_floated = iteration;
                  }
                }
              }
            }
          } else if (e->density < 0) {
            // Slide only if blocked above.
            if (y + 1 < h && points[x][y].last_floated != iteration) {
              Element* o = points[x][y + 1].element;
              if (o && (!o->mobile || e->density >= o->density)) {
                Element* p = (nx < 0 || nx >= w) ? NULL : points[nx][y].element;
                if (!p || (p->mobile && e->density < p->density && rng.NextFloat() < p->density - e->density)) {
                  Swap(x, y, nx, y);
                  if (nx >= 0 && nx < w) {
                    points[nx][y + 1].last_floated = iteration;
                  }
                }
              }
            }
          }
        }

        Element* o = points[x][y - 1].element;
        if ((!o && e->density > 0) || (o && o->mobile && e->density > o->density)) {
          if (!o || o->density == 0 || rng.NextFloat() < e->density - o->density) {
            Swap(x, y, x, y - 1);
            points[x][y].last_floated = iteration;
          }
          continue;
        }
        if (points[x][y].last_floated != iteration) {
          o = points[x][y + 1].element;
          if ((!o && e->density < 0) || (o && o->mobile && e->density < o->density)) {
            if (!o || o->density == 0 || rng.NextFloat() < o->density - e->density) {
              Swap(x, y, x, y + 1);
              if (y + 1 < h) {
                points[x][y + 1].last_floated = iteration;
              }
            }
          }
        }
      }
    }
  }

  jintArray GetPixels(JNIEnv* env) {
    jint* pixelData = (jint*) env->GetPrimitiveArrayCritical(pixels, NULL);
    jint* p = pixelData;
    for (jint y = h; y--; ) {
      for (jint x = 0; x < w; x++, p++) {
        *p = points[x][y].element ? points[x][y].element->color : 0xff000000;
      }
    }
    env->ReleasePrimitiveArrayCritical(pixels, pixelData, NULL);
    return (jintArray) env->NewLocalRef(pixels);
  }
};


Sandbox::SandboxList Sandbox::sandboxes;


extern "C" {

/*
 * Class:     com_loganh_sandblaster_NativeSandBox
 * Method:    setParticle
 * Signature: (IILcom/loganh/sandblaster/Element;)V
 */
void Java_com_loganh_sandblaster_NativeSandBox_setParticle__IILcom_loganh_sandblaster_Element_2(
    JNIEnv* env, jobject thiz, jint x, jint y, jobject jelement) {
  Sandbox* sandbox = Sandbox::Get(env, thiz);
  Element* element = sandbox->GetElement(env, jelement);
  sandbox->SetParticle(x, y, element);
}


/*
 * Class:     com_loganh_sandblaster_NativeSandBox
 * Method:    setParticle
 * Signature: (IILcom/loganh/sandblaster/Element;IF)V
 */
void Java_com_loganh_sandblaster_NativeSandBox_setParticle__IILcom_loganh_sandblaster_Element_2IF(
    JNIEnv* env, jobject thiz, jint x, jint y, jobject jelement, jint radius, jfloat prob) {
  Sandbox* sandbox = Sandbox::Get(env, thiz);
  Element* element = sandbox->GetElement(env, jelement);
  sandbox->SetParticle(x, y, element, radius, prob);
}


/*
 * Class:     com_loganh_sandblaster_NativeSandBox
 * Method:    line
 * Signature: (Lcom/loganh/sandblaster/Element;IIIII)V
 */
void Java_com_loganh_sandblaster_NativeSandBox_line__Lcom_loganh_sandblaster_Element_2IIIII(
    JNIEnv* env, jobject thiz, jobject jelement, jint radius, jint x1, jint y1, jint x2, jint y2) {
  Sandbox* sandbox = Sandbox::Get(env, thiz);
  Element* element = sandbox->GetElement(env, jelement);
  sandbox->Line(element, radius, x1, y1, x2, y2);
}


/*
 * Class:     com_loganh_sandblaster_NativeSandBox
 * Method:    line
 * Signature: (Lcom/loganh/sandblaster/Element;IIII)V
 */
void Java_com_loganh_sandblaster_NativeSandBox_line__Lcom_loganh_sandblaster_Element_2IIII(
    JNIEnv* env, jobject thiz, jobject jelement, jint x1, jint y1, jint x2, jint y2) {
  Sandbox* sandbox = Sandbox::Get(env, thiz);
  Element* element = sandbox->GetElement(env, jelement);
  sandbox->Line(element, x1, y1, x2, y2);
}


/*
 * Class:     com_loganh_sandblaster_NativeSandBox
 * Method:    update
 * Signature: ()V
 */
void Java_com_loganh_sandblaster_NativeSandBox_update(JNIEnv* env, jobject thiz) {
  Sandbox::Get(env, thiz)->Iterate();
}


/*
 * Class:     com_loganh_sandblaster_NativeSandBox
 * Method:    clear
 * Signature: ()V
 */
void Java_com_loganh_sandblaster_NativeSandBox_clear(JNIEnv* env, jobject thiz) {
  Sandbox::Get(env, thiz)->Clear();
}


/*
 * Class:     com_loganh_sandblaster_NativeSandBox
 * Method:    getPixels
 * Signature: ()[I
 */
jintArray Java_com_loganh_sandblaster_NativeSandBox_getPixels(JNIEnv* env, jobject thiz) {
  jintArray pixels = Sandbox::Get(env, thiz)->GetPixels(env);
  return pixels;
}

/*
 * Class:     com_loganh_sandblaster_NativeSandBox
 * Method:    readFromBytes
 * Signature: ([B)V
 */
void Java_com_loganh_sandblaster_NativeSandBox_readFromBytes(JNIEnv* env, jobject thiz, jbyteArray bytes) {
  Sandbox* sandbox = Sandbox::Get(env, thiz);
  // Instantiating a DataStream enters a critical section, so no JNI interaction until we delete it.
  DataStream* stream = new DataStream(env, bytes);
  sandbox->Read(stream);
  delete stream;
}

jint JNI_OnLoad(JavaVM* jvm, void* res) {
  return JNI_VERSION_1_2;
}

}
