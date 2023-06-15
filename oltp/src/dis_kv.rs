use std::collections::{HashMap, HashSet};

use rand::seq::IteratorRandom;
use rand::Rng;

use super::kv::{KVStoreT, OpId, SessionId, SetId, TxnId, Value, VarId};

use serde::ser::SerializeStruct;
use serde::{Serialize, Serializer};

use std::fs::File;
use std::io::BufWriter;

use std::collections::hash_map::DefaultHasher;
use std::hash::{Hash, Hasher};

use dbcop::consistency::Consistency;
use dbcop::db::history::{Event, Session, Transaction};
use dbcop::verifier::Verifier;

fn store_min_max<T>(val: &T, mn: &mut T, mx: &mut T)
where
    T: Ord + Copy,
{
    if val < mn {
        *mn = *val
    } else if val > mx {
        *mx = *val
    }
}

#[derive(Debug, Eq, PartialEq, Clone)]
pub enum Op {
    Write(VarId, Value, OpId),
    Read(VarId, OpId, OpId),
    Insert(SetId, VarId, OpId),
    Delete(SetId, VarId, OpId),
    Contains(SetId, VarId, OpId, OpId),
}

fn calculate_hash<T: Hash>(t: &T) -> u64 {
    let mut s = DefaultHasher::new();
    t.hash(&mut s);
    s.finish()
}

impl Serialize for Op {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: Serializer,
    {
        let (is_write, variable, value) = match self {
            Op::Write(var, _, c_opid) => (true, *var, calculate_hash(c_opid)),
            Op::Read(var, r_opid, _) => (false, *var, calculate_hash(r_opid)),
            Op::Insert(setid, var, c_opid) => {
                (true, calculate_hash(&(setid, var)), calculate_hash(c_opid))
            }
            Op::Delete(setid, var, c_opid) => {
                (true, calculate_hash(&(setid, var)), calculate_hash(c_opid))
            }
            Op::Contains(setid, var, r_opid, _) => {
                (false, calculate_hash(&(setid, var)), calculate_hash(r_opid))
            }
        };

        // 3 is the number of fields in the struct.
        let mut state = serializer.serialize_struct("Event", 4)?;
        state.serialize_field("write", &is_write)?;
        state.serialize_field("variable", &variable)?;
        state.serialize_field("value", &value)?;
        state.serialize_field("success", &true)?;
        state.end()
    }
}

impl From<&Op> for Event {
    fn from(op: &Op) -> Self {
        let (is_write, variable, value) = match op {
            Op::Write(var, _, c_opid) => (true, *var, calculate_hash(c_opid)),
            Op::Read(var, r_opid, _) => (false, *var, calculate_hash(r_opid)),
            Op::Insert(setid, var, c_opid) => {
                (true, calculate_hash(&(setid, var)), calculate_hash(c_opid))
            }
            Op::Delete(setid, var, c_opid) => {
                (true, calculate_hash(&(setid, var)), calculate_hash(c_opid))
            }
            Op::Contains(setid, var, r_opid, _) => {
                (false, calculate_hash(&(setid, var)), calculate_hash(r_opid))
            }
        };
        Event {
            write: is_write,
            variable: variable as usize,
            value: value as usize,
            success: true,
        }
    }
}

#[derive(Debug, Clone, Serialize)]
pub struct KVTransaction {
    t_id: TxnId,
    op: Vec<Op>,
    committed: bool,
}

impl KVTransaction {
    pub fn from_id(t_id: TxnId) -> Self {
        Self {
            t_id,
            op: vec![],
            committed: false,
        }
    }

    pub fn append_read(&mut self, x: &VarId, op_id: &OpId) {
        self.op
            .push(Op::Read(*x, *op_id, (self.t_id, self.op.len() as u64)));
    }

    pub fn append_contains(&mut self, s: &SetId, x: &VarId, op_id: &OpId) {
        self.op.push(Op::Contains(
            *s,
            *x,
            *op_id,
            (self.t_id, self.op.len() as u64),
        ));
    }

    #[allow(clippy::ptr_arg)]
    pub fn append_write(&mut self, x: &VarId, v: &Value) {
        self.op
            .push(Op::Write(*x, v.clone(), (self.t_id, self.op.len() as u64)));
    }

    pub fn append_insert(&mut self, s: &SetId, x: &VarId) {
        self.op
            .push(Op::Insert(*s, *x, (self.t_id, self.op.len() as u64)));
    }

    pub fn append_delete(&mut self, s: &SetId, x: &VarId) {
        self.op
            .push(Op::Delete(*s, *x, (self.t_id, self.op.len() as u64)));
    }

    pub fn pop(&mut self) {
        self.op.pop().unwrap();
    }

    pub fn commit(&mut self) {
        self.committed = true;
    }

    fn uncommit(&mut self) {
        self.committed = false;
    }

    fn rollback(&mut self) {
        self.op.drain(..);
    }

    pub fn contains(&self, s: &SetId, x: &VarId) -> Option<(bool, OpId)> {
        self.op
            .iter()
            .rev()
            .filter_map(|op| match op {
                Op::Insert(t, y, op_id) if s == t && x == y => Some((true, *op_id)),
                Op::Delete(t, y, op_id) if s == t && x == y => Some((false, *op_id)),
                _ => None,
            })
            .next()
    }

    pub fn read(&self, x: &VarId) -> Option<(Value, OpId)> {
        self.op
            .iter()
            .rev()
            .filter_map(|op| match op {
                Op::Write(x_, v, op_id) if x == x_ => Some((v.clone(), *op_id)),
                _ => None,
            })
            .next()
    }
}

#[derive(Default, Debug)]
pub struct KVStore {
    pub history: HashMap<SessionId, Vec<KVTransaction>>,
    pub read_strategy: String,
    consistency: Option<Consistency>,
}

impl KVStoreT for KVStore {
    fn commit(&mut self, s_id: &SessionId) -> bool {
        let entry = self.history.entry(*s_id).or_default();
        if let Some(txn) = entry.last_mut() {
            txn.commit();
        }
        entry.push(KVTransaction::from_id((*s_id, entry.len() as u64)));
        true
    }

    fn rollback(&mut self, s_id: &SessionId) -> bool {
        let entry = self.history.entry(*s_id).or_default();
        if let Some(txn) = entry.last_mut() {
            txn.rollback();
        }
        true
    }

    fn insert(&mut self, s: &SetId, x: &VarId, s_id: &SessionId) -> bool {
        let txn = self.get_last_transaction_mut(s_id);
        txn.append_insert(s, x);
        true
    }

    fn delete(&mut self, s: &SetId, x: &VarId, s_id: &SessionId) -> bool {
        let txn = self.get_last_transaction_mut(s_id);
        txn.append_delete(s, x);
        true
    }

    fn write_ser(&mut self, x: &VarId, v: &Value, s_id: &SessionId) -> bool {
        let txn = self.get_last_transaction_mut(s_id);
        txn.append_write(x, v);
        true
    }

    fn contains(&mut self, s: &SetId, x: &VarId, s_id: &SessionId) -> bool {
        let r_t_id = if let Some((_, op_id)) = self.get_last_transaction_mut(s_id).contains(s, x) {
            op_id.0
        } else {
            let mut choices: Vec<_> = self
                .history
                .values()
                .flat_map(|x| x.iter())
                .filter_map(|v| {
                    if v.committed && v.contains(s, x).is_some() {
                        Some(v.t_id)
                    } else {
                        None
                    }
                })
                .collect();

            if !choices.contains(&(0, 0)) {
                choices.push((0, 0));
            }

            // if choices != vec![(0, 0)] {
            //     println!("unfil choices contains {:?}", choices);
            // }

            choices = choices
                .drain(..)
                .filter(|r_t_id| {
                    if r_t_id == &(0, 0) {
                        let txn = self.get_transaction_mut(r_t_id);
                        if txn.op.iter().all(|op| match op {
                            Op::Delete(t, y, _) if s == t && x == y => false,
                            Op::Insert(t, y, _) if s == t && x == y => false,
                            _ => true,
                        }) {
                            txn.append_delete(s, x);
                        }
                    }

                    let op_id = match self.get_transaction(r_t_id).contains(s, x) {
                        Some((_, o_id)) => o_id,
                        None => {
                            assert!(r_t_id == &(0, 0));
                            (*r_t_id, 0)
                        }
                    };

                    {
                        let last_txn = self.get_last_transaction_mut(s_id);
                        last_txn.append_contains(s, x, &op_id);
                        last_txn.commit();
                    }
                    let valid = self.is_consistent();
                    {
                        let last_txn = self.get_last_transaction_mut(s_id);
                        last_txn.uncommit();
                        last_txn.pop();
                    }

                    valid
                })
                .collect();

            // if choices != vec![(0, 0)] {
            //     println!("contains - choices for {:?} {:?} - {:?}", s, x, choices);
            // }

            if choices.len() > 1 {
                self.choose_one_op(&choices)
            } else {
                choices[0]
            }
        };

        // println!("contain chose {:?}", r_t_id);

        let (val, r_o_id) = match self.get_transaction(&r_t_id).contains(s, x) {
            Some((v, op_id)) => (v, op_id),
            None => unreachable!(),
        };

        self.get_last_transaction_mut(s_id)
            .append_contains(s, x, &r_o_id);
        val
    }

    fn scan(&mut self, s: SetId, s_id: &SessionId) -> Vec<VarId> {
        let mut all_var = Vec::new();
        for &var in self.inserted_once(&s).iter() {
            if self.contains(&s, &var, s_id) {
                all_var.push(var);
            }
        }
        all_var
    }

    fn read_ser(&mut self, x: &VarId, s_id: &SessionId) -> Option<Value> {
        let r_t_id = if let Some((_, op_id)) = self.get_last_transaction_mut(s_id).read(x) {
            op_id.0
        } else {
            let mut choices: Vec<_> = self
                .history
                .values()
                .flat_map(|x| x.iter())
                .filter_map(|v| {
                    if v.committed && v.read(x).is_some() {
                        Some(v.t_id)
                    } else {
                        None
                    }
                })
                .collect();

            if !choices.contains(&(0, 0)) {
                choices.push((0, 0));
            }

            // if choices != vec![(0, 0)] {
            //     println!("unfil choices read {:?}", choices);
            // }

            choices = choices
                .drain(..)
                // .chain(std::iter::once(0))
                .filter(|r_t_id| {
                    // println!("trying to read from {:?}", r_id);

                    if r_t_id == &(0, 0) {
                        let txn = self.get_transaction_mut(r_t_id);
                        if txn
                            .op
                            .iter()
                            .all(|op| !matches!(op, Op::Write(y, _, _) if x == y))
                        {
                            txn.append_write(x, &vec![]);
                        }
                    }

                    let op_id = match self.get_transaction(r_t_id).read(x) {
                        Some((_, o_id)) => o_id,
                        None => {
                            assert!(r_t_id == &(0, 0));
                            (*r_t_id, 0)
                        }
                    };

                    {
                        let last_txn = self.get_last_transaction_mut(s_id);
                        last_txn.append_read(x, &op_id);
                        last_txn.commit();
                    }

                    // println!("{:?}", self.history);

                    let valid = self.is_consistent();

                    {
                        let last_txn = self.get_last_transaction_mut(s_id);
                        last_txn.uncommit();
                        last_txn.pop();
                    }

                    valid
                })
                .collect();

            // if choices != vec![(0, 0)] {
            //     println!("read - choices for {:?} - {:?}", x, choices);
            // }

            if choices.len() > 1 {
                self.choose_one_op(&choices)
            } else {
                choices[0]
            }
        };

        // println!("read chose {:?}", r_t_id);

        let (val, r_o_id) = match self.get_transaction(&r_t_id).read(x) {
            Some((v, op_id)) => (v, op_id),
            None => unreachable!(),
        };

        self.get_last_transaction_mut(s_id).append_read(x, &r_o_id);
        if val.is_empty() {
            None
        } else {
            Some(val)
        }
    }
}

impl KVStore {
    pub fn get_transaction(&self, t_id: &TxnId) -> &KVTransaction {
        let (s_id, t_id_) = t_id;
        let session = self.history.get(s_id).unwrap();
        session.get(*t_id_ as usize).unwrap()
    }

    pub fn get_transaction_mut(&mut self, t_id: &TxnId) -> &mut KVTransaction {
        let (s_id, t_id_) = t_id;
        let session = self.history.get_mut(s_id).unwrap();
        session.get_mut(*t_id_ as usize).unwrap()
    }

    pub fn get_last_transaction_mut(&mut self, s_id: &SessionId) -> &mut KVTransaction {
        let session = self.history.entry(*s_id).or_default();
        if session.is_empty() {
            session.push(KVTransaction::from_id((*s_id, 0)));
        }
        session.last_mut().unwrap()
    }

    pub fn get_last_transaction(&self, s_id: &SessionId) -> &KVTransaction {
        let session = self.history.get(s_id).unwrap();
        session.last().unwrap()
    }

    pub fn choose_one_op(&self, choices: &[TxnId]) -> TxnId {
        let mut rng = rand::thread_rng();
        let mut choices_txn: Vec<_> = {
            let mut choice_map: HashMap<_, HashSet<_>> = HashMap::new();
            for s_id in choices.iter() {
                let entry = choice_map.entry(s_id.0).or_default();
                entry.insert(s_id);
            }
            let session = *choice_map.keys().choose(&mut rng).unwrap();
            choice_map.get_mut(&session).unwrap().drain().collect()
        };
        *if choices_txn.len() > 1 {
            choices_txn.sort_unstable();
            if self.read_strategy == "weak" {
                println!("weak");
                *choices_txn.first().unwrap()
            } else if self.read_strategy == "latest" {
                println!("latest");
                *choices_txn.last().unwrap()
            } else if self.read_strategy == "weak biased" {
                println!("weak biased");
                for &t_id in choices_txn.iter() {
                    if rng.gen() {
                        return *t_id;
                    }
                }
                *choices_txn.last().unwrap()
            } else if self.read_strategy == "latest biased" {
                println!("latest biased");
                for &t_id in choices_txn.iter().rev() {
                    if rng.gen() {
                        return *t_id;
                    }
                }
                *choices_txn.first().unwrap()
            } else {
                // for any other just randomly pick
                println!("random {:?}", self.read_strategy);
                choices_txn.drain(..).choose(&mut rng).unwrap()
            }
        } else {
            choices_txn.first().unwrap()
        }
    }

    pub fn is_empty_session(&self, s_id: &SessionId) -> bool {
        self.history.get(s_id).unwrap().is_empty()
    }

    pub fn print_summary(&self) {
        let terminals: Vec<_> = self
            .history
            .iter()
            .filter_map(|(&k, v)| {
                if v.len() > 1 {
                    Some((k, v.len() - 1))
                } else {
                    None
                }
            })
            .collect();
        println!("terminal | no. of transactions");
        for (t, n_txn) in &terminals {
            println!("{} | {}", t, n_txn);
        }
        let mut read_min = u64::MAX;
        let mut read_max = 0u64;
        let mut write_min = u64::MAX;
        let mut write_max = 0u64;
        let mut insert_min = u64::MAX;
        let mut insert_max = 0u64;
        let mut delete_min = u64::MAX;
        let mut delete_max = 0u64;
        let mut contains_min = u64::MAX;
        let mut contains_max = 0u64;
        let mut txn_size_min = u64::MAX;
        let mut txn_size_max = 0u64;

        for (s_id, session) in self.history.iter() {
            if s_id == &0 {
                continue;
            }
            for txn in session {
                if txn.op.is_empty() {
                    continue;
                }
                let mut txn_size = 0;
                let mut n_write = 0;
                let mut n_read = 0;
                let mut n_insert = 0;
                let mut n_delete = 0;
                let mut n_contains = 0;
                for op in &txn.op {
                    txn_size += 1;
                    match op {
                        Op::Write(_, _, _) => n_write += 1,
                        Op::Read(_, _, _) => n_read += 1,
                        Op::Insert(_, _, _) => n_insert += 1,
                        Op::Delete(_, _, _) => n_delete += 1,
                        Op::Contains(_, _, _, _) => n_contains += 1,
                    }
                }

                store_min_max(&txn_size, &mut txn_size_min, &mut txn_size_max);
                store_min_max(&n_write, &mut write_min, &mut write_max);
                store_min_max(&n_read, &mut read_min, &mut read_max);
                store_min_max(&n_insert, &mut insert_min, &mut insert_max);
                store_min_max(&n_delete, &mut delete_min, &mut delete_max);
                store_min_max(&n_contains, &mut contains_min, &mut contains_max);
            }
        }

        println!("txn stat | min | max");
        println!("txn size | {} | {}", txn_size_min, txn_size_max);
        println!("# write | {} | {}", write_min, write_max);
        println!("# read | {} | {}", read_min, read_max);
        println!("# insert | {} | {}", insert_min, insert_max);
        println!("# delete | {} | {}", delete_min, delete_max);
        println!("# contains | {} | {}", contains_min, contains_max);
    }

    pub fn dump(&self, filepath: &str) {
        let file = File::create(filepath).unwrap();
        let buf = BufWriter::new(file);
        // TODO: unique write
        serde_json::to_writer_pretty(buf, &self.history).expect("error");
    }

    pub fn set_consistency(&mut self, st: &str) {
        self.consistency = Some(match st {
            "causal" => Consistency::Causal,
            "readcommitted" => Consistency::ReadCommitted,
            _ => unimplemented!(),
        });
        println!("set consistency to {:?}", self.consistency);
    }

    pub fn is_consistent(&self) -> bool {
        match self.consistency {
            None => self.is_cc_consistent(),
            Some(Consistency::Causal) => self.is_cc_consistent(),
            Some(Consistency::ReadCommitted) => self.is_read_committed_consistent(),
            Some(Consistency::Inc) => unreachable!(),
            Some(e) => self.is_consistent_with_level(Some(e), None),
        }
    }

    pub fn is_consistent_with_level(
        &self,
        minimum_level: Option<Consistency>,
        maximum_level: Option<Consistency>,
    ) -> bool {
        let consistency = self.minimal_consistency();
        let mut answer = true;
        if let Some(min_level) = minimum_level {
            answer &= min_level <= consistency;
        }
        if let Some(max_level) = maximum_level {
            answer &= consistency <= max_level;
        }
        answer
    }

    pub fn minimal_consistency(&self) -> Consistency {
        let mut history: Vec<Session> = Vec::new();

        for (_, session_deser) in self.history.iter() {
            let mut session = Vec::new();

            for txn_deser in session_deser {
                session.push(Transaction {
                    events: txn_deser.op.iter().map(|x| x.into()).collect(),
                    success: txn_deser.committed,
                })
            }

            history.push(session);
        }

        let tempdir = tempfile::tempdir().unwrap();

        let mut verifier = Verifier::new(tempdir.path().into());
        verifier.model("");

        match verifier.verify(&history) {
            Some(Consistency::RepeatableRead) => Consistency::ReadCommitted,
            Some(Consistency::ReadAtomic) => Consistency::RepeatableRead,
            Some(Consistency::Causal) => Consistency::ReadAtomic,
            Some(Consistency::Prefix) => Consistency::Causal,
            Some(Consistency::SnapshotIsolation) => Consistency::Prefix,
            Some(Consistency::Serializable) => Consistency::SnapshotIsolation,
            None => Consistency::Serializable,
            _ => unreachable!(),
        }
    }

    pub fn is_cc_consistent(&self) -> bool {
        let mut wr: HashMap<VarId, HashMap<TxnId, HashSet<TxnId>>> = Default::default();
        let mut ws: HashMap<VarId, HashSet<TxnId>> = Default::default();
        let mut wr_s: HashMap<(SetId, VarId), HashMap<TxnId, HashSet<TxnId>>> = Default::default();
        let mut ws_s: HashMap<(SetId, VarId), HashSet<TxnId>> = Default::default();
        let mut vis: HashMap<TxnId, HashSet<TxnId>> = Default::default();

        // assuming repeatable read and read committed is satisfied

        for sessions in self.history.values() {
            let mut prev_vis_txn = (0, 0);
            for kvtxn in sessions {
                for op in kvtxn.op.iter() {
                    match op {
                        Op::Read(x, rf_op_id, op_id) => {
                            // t1 reads x from t2
                            // println!("{:?} reads {:?} from {:?}", t1, x, t2);
                            if rf_op_id.0 != op_id.0 {
                                wr.entry(*x)
                                    .or_default()
                                    .entry(rf_op_id.0)
                                    .or_default()
                                    .insert(op_id.0);
                                vis.entry(rf_op_id.0).or_default().insert(op_id.0);
                            }
                        }
                        Op::Contains(s, x, rf_op_id, op_id) => {
                            // t1 reads Insert/Delete from t2
                            // println!("{:?} reads INS/DEL {:?} from {:?}", t1, x, t2);
                            if rf_op_id.0 != op_id.0 {
                                wr_s.entry((*s, *x))
                                    .or_default()
                                    .entry(rf_op_id.0)
                                    .or_default()
                                    .insert(op_id.0);
                                vis.entry(rf_op_id.0).or_default().insert(op_id.0);
                            }
                        }
                        Op::Write(x, _val, op_id) => {
                            // t1 writes x
                            // println!("{:?} writes {:?}", t1, x);
                            ws.entry(*x).or_default().insert(op_id.0);
                        }
                        Op::Insert(s, x, op_id) => {
                            // t1 Insert/Delete x
                            // println!("{:?} INS {:?} in {:?}", t1, x, s);
                            ws_s.entry((*s, *x)).or_default().insert(op_id.0);
                        }
                        Op::Delete(s, x, op_id) => {
                            // t1 Insert/Delete x
                            // println!("{:?} DEL {:?} from {:?}", t1, x, s);
                            ws_s.entry((*s, *x)).or_default().insert(op_id.0);
                        }
                    }
                }
                // session order
                if kvtxn.t_id != (0, 0) {
                    vis.entry(prev_vis_txn).or_default().insert(kvtxn.t_id);
                }
                prev_vis_txn = kvtxn.t_id;
            }
        }

        // for (k, v) in &self.history {
        //     if k != &0 {
        //         println!("{} -> {:?}", k, v);
        //         std::thread::sleep_ms(10_000);
        //     }
        // }

        // println!("{:?}", self.history);
        // println!("vis {:?}", vis);
        // std::thread::sleep_ms(10_000);
        // println!("wr {:?}", wr);
        // println!("ws {:?}", ws);

        saturate(&mut vis);

        let mut ww = Vec::new();

        for (x, t2t1) in wr.iter() {
            for (t2, t1s) in t2t1.iter() {
                for t1 in t1s.iter() {
                    if let Some(t3s) = ws.get(x) {
                        for t3 in t3s.iter() {
                            if t3 != t2 && vis.get(t3).map(|t3r| t3r.contains(t1)) == Some(true) {
                                // t2, t3 write on x
                                // t3 is visible to t1
                                // t1 reads x from t2
                                // t3 -> t2
                                // println!("ww {:?} -> {:?}", t3, t2);
                                ww.push((*t3, *t2));
                            }
                        }
                    }
                }
            }
        }

        for (x, t2t1) in wr_s.iter() {
            for (t2, t1s) in t2t1.iter() {
                for t1 in t1s.iter() {
                    if let Some(t3s) = ws_s.get(x) {
                        for t3 in t3s.iter() {
                            if t3 != t2 && vis.get(t3).map(|t3r| t3r.contains(t1)) == Some(true) {
                                // t2, t3 write on x
                                // t3 is visible to t1
                                // t1 reads x from t2
                                // t3 -> t2
                                // println!("ww {:?} -> {:?}", t3, t2);
                                ww.push((*t3, *t2));
                            }
                        }
                    }
                }
            }
        }

        // println!("ww {:?}", ww);
        // println!("vis {:?}", vis);

        // add write conflict for set operations.
        // Delete(_)

        for (u, v) in ww.drain(..) {
            vis.entry(u).or_default().insert(v);
        }

        saturate(&mut vis);

        // println!("cc-check");
        !vis.iter().any(|(k, v)| v.contains(k))
    }

    pub fn inserted_once(&self, s1: &SetId) -> Vec<VarId> {
        let mut all_vars = HashSet::new();
        for txn in self.history.values().flat_map(|x| x.iter()) {
            all_vars.extend(txn.op.iter().filter_map(|op| match op {
                Op::Insert(s2, v, _) if s1 == s2 => Some(v),
                _ => None,
            }))
        }
        all_vars.drain().collect()
    }

    /*
    (writes x)  wr
        t2 ----------> Beta
        |                |
     co |                | po
        |                |
        V                V
        t1 -----------> Alpha
               wr_x
     */
    pub fn is_read_committed_consistent(&self) -> bool {
        let mut ws: HashMap<VarId, HashSet<TxnId>> = Default::default();
        let mut ws_s: HashMap<(SetId, VarId), HashSet<TxnId>> = Default::default();
        let mut commit: HashMap<TxnId, HashSet<TxnId>> = Default::default();
        let mut so: HashMap<TxnId, HashSet<TxnId>> = Default::default();

        let mut wr: HashMap<VarId, HashMap<TxnId, HashSet<TxnId>>> = Default::default();
        let mut wr_s: HashMap<(SetId, VarId), HashMap<TxnId, HashSet<TxnId>>> = Default::default();

        // Populate read write relation data structures
        for session in self.history.values() {
            let mut prev_vis_txn = (0, 0);
            for txn in session {
                for op in txn.op.iter() {
                    match op {
                        Op::Read(x, rf_op_id, op_id) => {
                            // t1 reads x from t2
                            // println!("{:?} reads {:?} from {:?}", t1, x, t2);
                            if rf_op_id.0 != op_id.0 {
                                wr.entry(*x)
                                    .or_default()
                                    .entry(rf_op_id.0)
                                    .or_default()
                                    .insert(op_id.0);
                                commit.entry(rf_op_id.0).or_default().insert(op_id.0);
                            }
                        }
                        Op::Contains(s, x, rf_op_id, op_id) => {
                            // t1 reads Insert/Delete from t2
                            // println!("{:?} reads INS/DEL {:?} from {:?}", t1, x, t2);
                            if rf_op_id.0 != op_id.0 {
                                wr_s.entry((*s, *x))
                                    .or_default()
                                    .entry(rf_op_id.0)
                                    .or_default()
                                    .insert(op_id.0);
                                commit.entry(rf_op_id.0).or_default().insert(op_id.0);
                            }
                        }
                        Op::Write(x, _, o_id) => {
                            // t1 writes x
                            // println!("{:?} writes {:?}", t1, x);
                            ws.entry(*x).or_default().insert(o_id.0);
                        }
                        Op::Insert(s, x, o_id) => {
                            // t1 Insert/Delete x
                            // println!("{:?} INS {:?} in {:?}", t1, x, s);
                            ws_s.entry((*s, *x)).or_default().insert(o_id.0);
                        }
                        Op::Delete(s, x, o_id) => {
                            // t1 Insert/Delete x
                            // println!("{:?} DEL {:?} from {:?}", t1, x, s);
                            ws_s.entry((*s, *x)).or_default().insert(o_id.0);
                        }
                    }
                }
                if txn.t_id != (0, 0) {
                    so.entry(prev_vis_txn).or_default().insert(txn.t_id);
                }
                prev_vis_txn = txn.t_id;
            }
        }

        saturate(&mut so);

        for (x, t2t1) in wr.iter() {
            for (t2, t1s) in t2t1.iter() {
                for t1 in t1s.iter() {
                    if let Some(t3s) = ws.get(x) {
                        for t3 in t3s.iter() {
                            if t3 != t2 && so.get(t3).map(|t3r| t3r.contains(t1)) == Some(true) {
                                // t2, t3 write on x
                                // t3 is visible to t1
                                // t1 reads x from t2
                                // t3 -> t2
                                // println!("ww {:?} -> {:?}", t3, t2);
                                commit.entry(*t3).or_default().insert(*t2);
                            }
                        }
                    }
                }
            }
        }

        for (x, t2t1) in wr_s.iter() {
            for (t2, t1s) in t2t1.iter() {
                for t1 in t1s.iter() {
                    if let Some(t3s) = ws_s.get(x) {
                        for t3 in t3s.iter() {
                            if t3 != t2 && so.get(t3).map(|t3r| t3r.contains(t1)) == Some(true) {
                                // t2, t3 write on x
                                // t3 is visible to t1
                                // t1 reads x from t2
                                // t3 -> t2
                                // println!("ww {:?} -> {:?}", t3, t2);
                                commit.entry(*t3).or_default().insert(*t2);
                            }
                        }
                    }
                }
            }
        }

        // Check alpha, beta pair of operations in program order which satisfy read committed property
        // Transaction containing beta should do write (or insert/delete) to x on which alpha does read (or contain)
        for session in self.history.values() {
            for txn in session {
                for (b_index, beta) in txn.op.iter().enumerate() {
                    match beta {
                        Op::Read(_, rf_o2_id, o2_id) | Op::Contains(_, _, rf_o2_id, o2_id) => {
                            if rf_o2_id.0 != o2_id.0 {
                                for alpha in &txn.op[b_index + 1..] {
                                    match alpha {
                                        Op::Read(x, o1_id, _) => {
                                            // Check t2 writes x through write operation
                                            if o1_id.0 != rf_o2_id.0
                                                && ws.get(x).map(|t2s| t2s.contains(&rf_o2_id.0))
                                                    == Some(true)
                                            {
                                                // t2 commits before t1
                                                commit
                                                    .entry(rf_o2_id.0)
                                                    .or_default()
                                                    .insert(o1_id.0);
                                            }
                                        }
                                        Op::Contains(s, x, o1_id, _) => {
                                            // Check t2 writes x through insert/delete operation
                                            if o1_id.0 != rf_o2_id.0
                                                && ws_s
                                                    .get(&(*s, *x))
                                                    .map(|t2s| t2s.contains(&rf_o2_id.0))
                                                    == Some(true)
                                            {
                                                // t2 commits before t1
                                                commit
                                                    .entry(rf_o2_id.0)
                                                    .or_default()
                                                    .insert(o1_id.0);
                                            }
                                        }
                                        _ => (),
                                    }
                                }
                            }
                        }
                        _ => (),
                    }
                }
            }
        }

        for (k, v_) in so.iter() {
            if let Some(v) = commit.get_mut(k) {
                v.extend(v_.iter());
            }
        }

        // println!("commit {:?}", commit);
        // If there is any cycle in committed txs, state is inconsistent
        saturate(&mut commit);
        let result: bool = !commit.iter().any(|(k, v)| v.contains(k));
        // println!("commit after saturate {:?}", commit);
        // println!("result {:?}", result);
        result
    }
}

fn saturate_util(u: &TxnId, g: &mut HashMap<TxnId, HashSet<TxnId>>, seen: &mut HashSet<TxnId>) {
    if seen.insert(*u) {
        let u_n = g.get(u).cloned();

        if let Some(ref vs) = u_n {
            for v in vs.iter() {
                saturate_util(v, g, seen)
            }
        }

        if let Some(vs) = u_n {
            for v in vs.iter() {
                if let Some(ref v_s) = g.get(v).cloned() {
                    let u_s = g.entry(*u).or_default();
                    *u_s = u_s.union(v_s).cloned().collect();
                }
            }
        }
    }
}

fn saturate(g: &mut HashMap<TxnId, HashSet<TxnId>>) {
    let mut seen = Default::default();
    let keys: Vec<_> = g.keys().cloned().collect();
    for u in keys.iter() {
        saturate_util(u, g, &mut seen);
    }
}
