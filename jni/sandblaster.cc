#include <jni.h>
#include <stdlib.h>
#include <time.h>

#include <android/log.h>

#define LOG(...) __android_log_write(ANDROID_LOG_INFO, "com.loganh.sandblaster", __VA_ARGS__)

extern JavaVM* jvm;

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
    return (random() & (1 << 10)) != 0;
  }

  jfloat NextFloat() {
    return (float) random() / RAND_MAX;
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

struct ProductSet {
  jint size;
  Element* products;
  jfloat* weights;
  jfloat total_weight;
};


struct Element {
  jint color;
  jboolean mobile;
  jfloat density;
  jfloat viscosity;
  jint lifetime;
  jint transmutationCount;
  jfloat decay_probability;
  ProductSet decay_product;
};


// TODO: ndk hack!
// hard-code the elements
static Element **defaultElements;

static void initDefaultElements() {
  if (!defaultElements) {
    defaultElements = new Element*[3];
    Element* e = defaultElements[0] = new Element();
    e->color = 0xffaaaaaa;  // wall grey
    e = defaultElements[1] = new Element();
    e->color = 0xffffff00;  // sand yellow
    e->mobile = true;
    e->density = 0.5;
    e->viscosity = 0.4;
    e = defaultElements[2] = new Element();
    e->color = 0xff0000ff;  // water blue
    e->mobile = true;
    e->density = 0.4;
    e->viscosity = 1.0;
    defaultElements[3] = NULL;
  }
}
// end of hack

struct Transmutation {
  jfloat probability;
  Element* target;
  ProductSet* product;
};


struct Point {
  Element* element;
  jint age;
  jint last_set;
  jint last_changed;
  jint last_floated;

  Point& operator=(Element* element) {
    this->element = element;
    age = 0;
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

  // 2D map of element pointers. NULL means no particle.
  Grid<Point> points;

  // 1D array representation of the resulting bitmap.
  jintArray pixels;
  jintArray pixelsGlobalRef;
  

  Sandbox(JNIEnv* env, jint w, jint h, jobject java_ref) : points(w, h) {
    this->w = w;
    this->h = h;
    this->java_ref = java_ref;
    // FIXME: I don't know how to release this.
    jintArray localPixelsRef = (jintArray) env->NewIntArray(w * h);
    pixels = (jintArray) env->NewGlobalRef(localPixelsRef);
    env->DeleteLocalRef(localPixelsRef);
  }

  ~Sandbox() {
    JNIEnv *env;
    jvm->AttachCurrentThread(&env, NULL);
    env->DeleteGlobalRef(pixels);
    env->DeleteGlobalRef(java_ref);
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
    // TODO: ndk hack!
    static jfieldID colorFid;
    if (!colorFid) {
      jclass cls = env->GetObjectClass(jelement);
      colorFid = env->GetFieldID(cls, "color", "I");
    }
    jint color = env->GetIntField(jelement, colorFid);
    for (Element** e = defaultElements; *e; e++) {
      if ((*e)->color == color) {
        return *e;
      }
    }
    return 0;
  }

  void SetParticle(jint x, jint y, Element* elem) {
    points[x][y] = elem;
    points[x][y].last_changed = iteration;
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
      Point p1 = points[x1][y1];
      points[x1][y1] = points[x2][y2];
      points[x2][y2] = p1;
    }
  }

  void Iterate() {
    // TODO: sources

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

        // TODO: Transmutations
        /*
        // Transmutations.
        if (e.transmutationCount > 0 && curLastSet != iteration) {
          for (int i = 0; i < NEIGHBORS.length; i++) {
            int nx = x + NEIGHBORS[i][0];
            int ny = y + NEIGHBORS[i][1];
            if (nx >= 0 && nx < width && ny >= 0 & ny < height && lastSet[nx][ny] != iteration) {
              Element t = elements[nx][ny];
              if (t != null) {
                Element o = elementTable.maybeTransmutate(e, t);
                if (o != elements[nx][ny]) {
                  setParticle(nx, ny, o);
                }
              }
            }
          }
        }
        */

        // TODO: Decay
        /*
        // Decay.
        if (curLastSet != iteration && e.decayProbability > 0 && RNG.nextFloat() < e.decayProbability) {
          ages[x][y]++;
          if (ages[x][y] > e.lifetime) {
            setParticle(x, y, e.decayProducts == null ? null : e.decayProducts.pickProduct());
            continue;
          }
        }
        */

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
    jint *pixelData = env->GetIntArrayElements(pixels, NULL);
    jint *p = pixelData;
    for (jint y = h; y--; ) {
      for (jint x = 0; x < w; x++, p++) {
        *p = points[x][y].element ? points[x][y].element->color : 0xff000000;
      }
    }
    env->ReleaseIntArrayElements(pixels, pixelData, 0);
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
  return Sandbox::Get(env, thiz)->GetPixels(env);
}

jint JNI_OnLoad(JavaVM* jvm, void* res) {
  initDefaultElements();
  return JNI_VERSION_1_2;
}

}
