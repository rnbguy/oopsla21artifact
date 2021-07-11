use std::collections::HashMap;

use super::kv::{KVStoreT, SessionId, SetId, Value, VarId};

#[derive(Default, Debug)]
pub struct KVStore {
    pub map: HashMap<VarId, Value>,
    pub setmap: HashMap<SetId, HashMap<VarId, bool>>,
}

impl KVStoreT for KVStore {
    fn commit(&mut self, _: &SessionId) -> bool {
        true
    }

    fn rollback(&mut self, _: &SessionId) -> bool {
        false
    }

    fn insert(&mut self, s: &SetId, x: &VarId, _: &SessionId) -> bool {
        // println!("INSERT {:?} {:?}", s, x);
        let ent = self.setmap.entry(*s).or_default();
        ent.insert(*x, true);
        true
    }

    fn delete(&mut self, s: &SetId, x: &VarId, _: &SessionId) -> bool {
        // println!("DELETE {:?} {:?}", s, x);
        let ent = self.setmap.entry(*s).or_default();
        ent.insert(*x, false);
        true
    }

    fn write_ser(&mut self, x: &VarId, v: &Value, _: &SessionId) -> bool {
        // println!("WRITE {:?} {:?}", x, v);
        self.map.insert(*x, v.clone());
        true
    }

    fn contains(&mut self, s: &SetId, x: &VarId, _: &SessionId) -> bool {
        match self.setmap.get(s) {
            Some(v) => v.get(x) == Some(&true),
            None => false,
        }
    }

    fn scan(&mut self, sid: SetId, _: &SessionId) -> Vec<VarId> {
        match self.setmap.get(&sid) {
            Some(v) => v
                .iter()
                .filter_map(|(&k, &v)| if v { Some(k) } else { None })
                .collect(),
            None => vec![],
        }
    }

    fn read_ser(&mut self, x: &VarId, _: &SessionId) -> Option<Value> {
        // println!("READ {:?}", x);
        self.map.get(x).cloned()
    }
}
